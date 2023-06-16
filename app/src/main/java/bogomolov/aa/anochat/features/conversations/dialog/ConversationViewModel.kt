package bogomolov.aa.anochat.features.conversations.dialog

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.AudioPlayer
import bogomolov.aa.anochat.features.shared.BitmapWithName
import bogomolov.aa.anochat.features.shared.LocaleProvider
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.Event
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.FileTooBigException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class UserStatus(open val time: Long? = null) {
    object Empty : UserStatus()
    object Online : UserStatus()
    object Typing : UserStatus()
    data class LastSeen(override val time: Long) : UserStatus(time)
}

enum class InputStates {
    INITIAL,
    TEXT_ENTERED,
    FAB_EXPAND,
    VOICE_RECORDING,
    VOICE_RECORDED
}

data class DialogUiState(
    val conversation: Conversation? = null,
    val userStatus: UserStatus = UserStatus.Empty,
    val inputState: InputStates = InputStates.INITIAL,
    val replyMessage: Message? = null,
    val audioFile: String? = null,
    val photoPath: String? = null,
    val text: String = "",
    val audioLengthText: String = "",
    val playingState: PlayingState? = null,
    val pagingDataFlow: Flow<PagingData<Any>>? = null,
    val selectedMessages: List<Message> = listOf(),

    val resized: BitmapWithName? = null,
    val isVideo: Boolean = false,
    val progress: Float = 0f
)

data class PlayingState(
    val audioFile: String,
    val duration: Long = 0,
    val elapsed: Long = 0,
    val messageId: String? = null,
    val paused: Boolean = false
)

data class SendMessageData(
    val text: String? = null,
    val audio: String? = null,
    val image: String? = null,
    val video: String? = null
)

object OnMessageSent : Event
object FileTooBig : Event

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val conversationUseCases: ConversationUseCases,
    private val messageUseCases: MessageUseCases,
    private val audioPlayer: AudioPlayer,
    private val localeProvider: LocaleProvider,
    private val fileStore: FileStore
) : BaseViewModel<DialogUiState>(DialogUiState()) {
    private var recordingJob: Job? = null
    private var playJob: Job? = null
    private var startTime = 0L
    private var tempElapsed = 0L
    private var conversationInitialized = false
    private var typingJob: Job? = null
    var uri: Uri? = null

    override fun onCleared() {
        currentState.conversation?.id?.let {
            conversationUseCases.deleteConversationIfNoMessages(it)
        }
        super.onCleared()
    }

    fun resizeMedia(mediaUri: Uri?, mediaPath: String?, isVideo: Boolean) {
        viewModelScope.launch {
            try {
                updateState { copy(resized = null, isVideo = isVideo) }
                val resized = if (isVideo) fileStore.resizeVideo(mediaUri!!)
                else fileStore.resizeImage(mediaUri, mediaPath, toGallery = (mediaUri == null))
                updateState { copy(resized = resized, isVideo = isVideo) }
                resized?.progress?.collect {
                    updateState { copy(progress = it.toFloat() / 100) }
                }
            } catch (e: FileTooBigException) {
                addEvent(FileTooBig)
            }
        }
    }

    fun messageDisplayed(message: Message) {
        viewModelScope.launch {
            currentState.conversation?.user?.uid?.let { uid ->
                messageUseCases.messageDisplayed(message, uid)
            }
        }
    }

    fun textChanged(enteredText: String) {
        viewModelScope.launch {
            if (enteredText.isNotEmpty()) {
                setState {
                    copy(text = enteredText, inputState = InputStates.TEXT_ENTERED)
                }
            } else {
                setState { copy(text = "", inputState = InputStates.INITIAL) }
            }
            currentState.conversation?.user?.uid?.let {
                if (typingJob == null) messageUseCases.startTypingTo(it)
                typingJob?.cancel()
                typingJob = launch {
                    delay(3000)
                    messageUseCases.stopTypingTo(it)
                    typingJob = null
                }
            }
        }
    }

    fun startPlaying(audioFile: String? = null, messageId: String? = null) {
        viewModelScope.launch(dispatcher) {
            if (currentState.playingState == null) initStartPlaying(audioFile, messageId)
            if (audioPlayer.startPlay()) {
                startTime = System.currentTimeMillis()
                playJob = launch {
                    while (true) {
                        delay(1000)
                        val time = System.currentTimeMillis() - startTime + tempElapsed
                        setState { copy(playingState = playingState?.copy(elapsed = time)) }
                        if (!isActive) break
                    }
                }
                setState { copy(playingState = playingState?.copy(paused = false)) }
            }
        }
    }

    private fun initStartPlaying(audioFile: String? = null, messageId: String? = null) {
        if (audioFile != null) {
            val duration = audioPlayer.initPlayer(audioFile) {
                playJob?.cancel()
                updateState { copy(playingState = null) }
            }
            val newPlayingState =
                PlayingState(audioFile = audioFile, duration = duration, messageId = messageId)
            setState { copy(playingState = newPlayingState) }
        }
    }

    fun pausePlaying() {
        if (audioPlayer.pausePlay()) {
            tempElapsed += System.currentTimeMillis() - startTime
            playJob?.cancel()
            val playingState = currentState.playingState?.copy(paused = true)
            setState { copy(playingState = playingState) }
        }
    }

    fun startRecording() {
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch(dispatcher) {
            val audioFile = audioPlayer.startRecording()
            setState { copy(audioFile = audioFile, inputState = InputStates.VOICE_RECORDING) }
            val startTime = System.currentTimeMillis()
            while (true) {
                val time = System.currentTimeMillis() - startTime
                val timeString = SimpleDateFormat("mm:ss").format(Date(time))
                setState { copy(audioLengthText = timeString) }
                delay(1000)
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch(dispatcher) {
            audioPlayer.stopRecording()
            recordingJob?.cancel()
            setState { copy(inputState = InputStates.VOICE_RECORDED) }
        }
    }

    fun sendMessage(data: SendMessageData) {
        viewModelScope.launch {
            currentState.conversation?.let {
                val replyId = currentState.replyMessage?.messageId
                val message = Message(
                    text = data.text ?: "",
                    isMine = true,
                    conversationId = it.id,
                    replyMessageId = replyId,
                    audio = data.audio,
                    image = data.image,
                    video = data.video
                )
                setState {
                    copy(
                        inputState = InputStates.INITIAL,
                        text = "",
                        replyMessage = null,
                        photoPath = null,
                        audioFile = null,
                    )
                }
                messageUseCases.sendMessage(message, it.user.uid)
                addEvent(OnMessageSent)
            }
        }
    }

    fun deleteMessages() {
        viewModelScope.launch {
            messageUseCases.deleteMessages(currentState.selectedMessages.map { it.id }.toSet())
            clearMessages()
        }
    }

    fun clearMessages() {
        updateState { copy(selectedMessages = listOf()) }
    }

    fun selectMessage(message: Message) {
        updateState {
            copy(selectedMessages = selectedMessages.toMutableList().apply {
                if (!contains(message)) {
                    add(message)
                } else {
                    remove(message)
                }
            })
        }
    }

    fun initConversation(conversationId: Long) {
        viewModelScope.launch(dispatcher) {
            if (!conversationInitialized) {
                conversationInitialized = true
                val conversation = conversationUseCases.getConversation(conversationId)!!
                setState { copy(conversation = conversation) }
                launch {
                    subscribeToOnlineStatus(conversation.user.uid)
                }
                subscribeToMessages(conversation)
            }
        }
    }

    private fun subscribeToMessages(conversation: Conversation) {
        val pagingDataFlow = messageUseCases.loadMessagesDataSource(conversation.id)
            .cachedIn(viewModelScope).map {
                it.insertSeparators { m1, m2 ->
                    insertDateSeparators(m1, m2, localeProvider.locale)?.let { DateDelimiter(it) }
                }
            }
        setState { copy(pagingDataFlow = pagingDataFlow) }
    }

    private suspend fun subscribeToOnlineStatus(uid: String) {
        userUseCases.addUserStatusListener(uid).collect {
            val typing = it.first
            val online = it.second
            val lastSeenTime = it.third
            val status = if (typing) UserStatus.Typing
            else if (online) UserStatus.Online else UserStatus.LastSeen(lastSeenTime)
            setState { copy(userStatus = status) }
        }
    }
}

private fun insertDateSeparators(message1: Message?, message2: Message?, locale: Locale): String? {
    if (message1 != null) {
        val day1 = GregorianCalendar().apply { time = Date(message1.time) }.get(Calendar.DAY_OF_YEAR)
        if (message2 != null) {
            val day2 = GregorianCalendar().apply { time = Date(message2.time) }.get(Calendar.DAY_OF_YEAR)
            if (day1 != day2)
                return SimpleDateFormat("dd MMMM yyyy", locale).format(Date(message1.time))
        } else {
            return SimpleDateFormat("dd MMMM yyyy", locale).format(Date(message1.time))
        }
    }
    return null
}