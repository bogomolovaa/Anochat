package bogomolov.aa.anochat.features.conversations.dialog

import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.Event
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.FileTooBigException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

data class DialogState(
    val conversation: Conversation? = null,
    val userStatus: UserStatus = UserStatus.Empty,
    val playingState: PlayingState? = null,
    val selectedMessages: ImmutableList<Message> = ImmutableList(listOf()),
    val inputState: InputState = InputState(),
    val replyMessage: Message? = null,
    val messagesFlow: ImmutableFlow<PagingData<Any>>? = null,

    val resized: BitmapWithName? = null,
    val isVideo: Boolean = false,
    val progress: Float = 0f,
    val resizingFinished: Boolean = false
)

data class InputState(
    val state: State = State.INITIAL,
    val text: String = "",
    val photoPath: String? = null,
    val audioFile: String? = null,
    val audioLengthText: String = ""
) {
    enum class State {
        INITIAL,
        TEXT_ENTERED,
        FAB_EXPAND,
        VOICE_RECORDING,
        VOICE_RECORDED
    }
}

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
object MessageSubmitted : Event
object VideoIsProcessing : Event

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val conversationUseCases: ConversationUseCases,
    private val messageUseCases: MessageUseCases,
    private val audioPlayer: AudioPlayer,
    private val localeProvider: LocaleProvider,
    private val fileStore: FileStore
) : BaseViewModel<DialogState>(DialogState()) {
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

    fun resizeMedia(mediaUri: Uri?, isVideo: Boolean) {
        viewModelScope.launch {
            try {
                setState { copy(resized = null, isVideo = isVideo) }
                val resized = if (isVideo) fileStore.resizeVideo(mediaUri!!) { progress, finished ->
                    setState { copy(progress = progress.toFloat() / 100, resizingFinished = finished) }
                }
                else fileStore.resizeImage(mediaUri, currentState.inputState.photoPath, toGallery = (mediaUri == null))
                setState { copy(resized = resized, isVideo = isVideo) }
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
                    copy(inputState = inputState.copy(state = InputState.State.TEXT_ENTERED, text = enteredText))
                }
            } else {
                setState { copy(inputState = inputState.copy(state = InputState.State.INITIAL, text = "")) }
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

    fun play(audioFile: String?, messageId: String?) {
        if (currentState.playingState?.paused != false) {
            startPlaying(audioFile, messageId)
        } else {
            pausePlaying()
        }
    }

    fun clearReplyMessage() {
        setState { copy(replyMessage = null) }
    }

    private fun startPlaying(audioFile: String? = null, messageId: String? = null) {
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
                setState { copy(playingState = null) }
            }
            val newPlayingState =
                PlayingState(audioFile = audioFile, duration = duration, messageId = messageId)
            setState { copy(playingState = newPlayingState) }
        }
    }

    private fun pausePlaying() {
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
            setState {
                copy(
                    inputState = inputState.copy(
                        state = InputState.State.VOICE_RECORDING,
                        audioFile = audioFile
                    )
                )
            }
            val startTime = System.currentTimeMillis()
            while (true) {
                val time = System.currentTimeMillis() - startTime
                val timeString = SimpleDateFormat("mm:ss").format(Date(time))
                setState { copy(inputState = inputState.copy(audioLengthText = timeString)) }
                delay(1000)
            }
        }
    }

    private fun stopRecording() {
        viewModelScope.launch(dispatcher) {
            audioPlayer.stopRecording()
            recordingJob?.cancel()
            setState { copy(inputState = inputState.copy(state = InputState.State.VOICE_RECORDED)) }
        }
    }

    private fun sendMessage(data: SendMessageData) {
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
                setState { copy(inputState = InputState(), replyMessage = null) }
                messageUseCases.sendMessage(message, it.user.uid)
                addEvent(OnMessageSent)
            }
        }
    }

    fun submitMedia() {
        val resized = currentState.resized
        val isVideo = currentState.isVideo
        val finished = currentState.resizingFinished
        val text = currentState.inputState.text
        viewModelScope.launch {
            if (finished && resized != null) {
                if (isVideo) {
                    sendMessage(SendMessageData(video = nameToVideo(resized.name), text = text))
                } else {
                    sendMessage(SendMessageData(image = nameToImage(resized.name), text = text))
                }
                addEvent(MessageSubmitted)
            } else {
                addEvent(VideoIsProcessing)
            }
        }
    }

    fun setPhotoPath(photoPath: String) {
        setState { copy(inputState = inputState.copy(photoPath = photoPath)) }
    }

    fun setReplyMessage(message: Message) {
        setState { copy(replyMessage = message) }
    }

    fun fabPressed() {
        when (currentState.inputState.state) {
            InputState.State.INITIAL -> setState { copy(inputState = inputState.copy(state = InputState.State.FAB_EXPAND)) }
            InputState.State.FAB_EXPAND -> setState { copy(inputState = inputState.copy(state = InputState.State.INITIAL)) }
            InputState.State.TEXT_ENTERED -> sendMessage(SendMessageData(text = currentState.inputState.text))
            InputState.State.VOICE_RECORDED -> sendMessage(SendMessageData(audio = currentState.inputState.audioFile))
            InputState.State.VOICE_RECORDING -> stopRecording()
        }
    }

    fun resetInputState() {
        setState { copy(inputState = inputState.copy(state = InputState.State.INITIAL, audioFile = null)) }
    }

    fun deleteMessages() {
        viewModelScope.launch {
            messageUseCases.deleteMessages(currentState.selectedMessages.map { it.id }.toSet())
            clearMessages()
        }
    }

    fun clearMessages() {
        setState { copy(selectedMessages = ImmutableList(listOf())) }
    }

    fun selectMessage(message: Message) {
        setState {
            copy(selectedMessages = selectedMessages.toMutableList().apply {
                if (!contains(message)) {
                    add(message)
                } else {
                    remove(message)
                }
            }.asImmutableList())
        }
    }

    fun initConversation(conversationId: Long) {
        viewModelScope.launch(dispatcher) {
            if (!conversationInitialized) {
                conversationInitialized = true
                subscribeToMessages(conversationId)
                conversationUseCases.getConversation(conversationId)?.let {
                    setState { copy(conversation = it) }
                    launch {
                        subscribeToOnlineStatus(it.user.uid)
                    }
                }
            }
        }
    }

    private fun subscribeToMessages(conversationId: Long) {
        val pagingDataFlow = messageUseCases.loadMessagesDataSource(conversationId)
            .cachedIn(viewModelScope).map {
                it.insertSeparators { m1, m2 ->
                    insertDateSeparators(m1, m2, localeProvider.locale)?.let { DateDelimiter(it) }
                }
            }.asImmutableFlow()
        updateState { copy(messagesFlow = pagingDataFlow.asImmutableFlow()) }
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