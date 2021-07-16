package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.net.Uri
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

const val ONLINE_STATUS = "online"
const val TYPING_STATUS = "typing..."

enum class InputStates {
    INITIAL,
    TEXT_ENTERED,
    FAB_EXPAND,
    VOICE_RECORDING,
    VOICE_RECORDED
}

data class DialogUiState(
    val conversation: Conversation? = null,
    val onlineStatus: String = "",
    val inputState: InputStates = InputStates.INITIAL,
    val replyMessage: Message? = null,
    val audioFile: String? = null,
    val photoPath: String? = null,
    val text: String = "",
    val audioLengthText: String = "",
    val playingState: PlayingState? = null,
    val pagingData: PagingData<MessageViewData>? = null,
    val pagingDataFlow: Flow<PagingData<MessageViewData>>? = null,

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

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val conversationUseCases: ConversationUseCases,
    private val messageUseCases: MessageUseCases,
    private val audioPlayer: AudioPlayer,
    private val localeProvider: LocaleProvider
) : BaseViewModel<DialogUiState>(DialogUiState()) {
    private var recordingJob: Job? = null
    private var playJob: Job? = null
    private var startTime = 0L
    private var tempElapsed = 0L
    private var conversationInitialized = false
    private var typingJob: Job? = null

    @Inject
    lateinit var fileStore: FileStore

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(dispatcher) {
            currentState.conversation?.id?.let {
                conversationUseCases.deleteConversationIfNoMessages(
                    it
                )
            }
        }
    }


    fun resizeMedia(mediaUri: Uri?, mediaPath: String?, isVideo: Boolean) = execute {
        updateState { copy(resized = null, isVideo = isVideo) }
        val resized = if (isVideo) fileStore.resizeVideo(mediaUri!!, viewModelScope)
        else fileStore.resizeImage(mediaUri, mediaPath, toGallery = (mediaUri == null))
        updateState { copy(resized = resized, isVideo = isVideo) }
        resized?.progress?.collect {
            updateState { copy(progress = it.toFloat() / 100) }
        }
    }

    fun notifyAsViewed(messages: List<MessageViewData>) = execute {
        currentState.conversation?.user?.uid?.let { uid ->
            messageUseCases.notifyAsViewed(messages.map { it.message }, uid)
        }
    }

    fun textChanged(enteredText: String) = execute {
        val uid = currentState.conversation?.user?.uid
        if (uid != null) {
            if (typingJob == null) messageUseCases.startTypingTo(uid)
            typingJob?.cancel()
            typingJob = GlobalScope.launch(dispatcher) {
                delay(3000)
                messageUseCases.stopTypingTo(uid)
                typingJob = null
            }
        }
        if (enteredText.isNotEmpty()) {
            setState {
                copy(text = enteredText, inputState = InputStates.TEXT_ENTERED)
            }
        } else {
            setState { copy(text = "", inputState = InputStates.INITIAL) }
        }
    }

    fun startPlaying(audioFile: String? = null, messageId: String? = null) = execute {
        if (currentState.playingState == null) initStartPlaying(audioFile, messageId)
        if (audioPlayer.startPlay()) {
            startTime = System.currentTimeMillis()
            playJob = viewModelScope.launch(dispatcher) {
                while (true) {
                    delay(1000)
                    val time = System.currentTimeMillis() - startTime + tempElapsed
                    setState { copy(playingState = playingState?.copy(elapsed = time)) }
                }
            }
            setState { copy(playingState = playingState?.copy(paused = false)) }
        }
    }

    private suspend fun initStartPlaying(audioFile: String? = null, messageId: String? = null) {
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

    fun pausePlaying() = execute {
        if (audioPlayer.pausePlay()) {
            tempElapsed += System.currentTimeMillis() - startTime
            playJob?.cancel()
            val playingState = currentState.playingState?.copy(paused = true)
            setState { copy(playingState = playingState) }
        }
    }

    fun startRecording() = execute {
        val audioFile = audioPlayer.startRecording()
        setState { copy(audioFile = audioFile, inputState = InputStates.VOICE_RECORDING) }
        recordingJob = viewModelScope.launch(dispatcher) {
            val startTime = System.currentTimeMillis()
            while (true) {
                val time = System.currentTimeMillis() - startTime
                val timeString = SimpleDateFormat("mm:ss").format(Date(time))
                setState { copy(audioLengthText = timeString) }
                delay(1000)
            }
        }
    }

    fun stopRecording() = execute {
        audioPlayer.stopRecording()
        recordingJob?.cancel()
        setState { copy(inputState = InputStates.VOICE_RECORDED) }
    }

    fun sendMessage(data: SendMessageData) = execute {
        val conversation = currentState.conversation
        if (conversation != null) {
            val replyId = currentState.replyMessage?.messageId
            val message = Message(
                text = data.text ?: "",
                time = System.currentTimeMillis(),
                isMine = true,
                conversationId = conversation.id,
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
            messageUseCases.sendMessage(message, conversation.user.uid)
            addEvent(OnMessageSent)
        }
    }

    fun deleteMessages(ids: Set<Long>) = execute {
        messageUseCases.deleteMessages(ids)
    }

    fun initConversation(conversationId: Long) = execute {
        if (!conversationInitialized) {
            conversationInitialized = true
            val conversation = conversationUseCases.getConversation(conversationId)!!
            setState { copy(conversation = conversation) }
            subscribeToMessages(conversation)
            subscribeToOnlineStatus(conversation.user.uid)
        }
    }

    private fun subscribeToMessages(conversation: Conversation) = execute {
        val pagingDataFlow = messageUseCases.loadMessagesDataSource(conversation.id).flowOn(dispatcher)
            .cachedIn(viewModelScope.plus(dispatcher)).map {
                it.map { MessageViewData(it) }.insertSeparators { m1, m2 ->
                    insertDateSeparators(m1, m2, localeProvider.locale)
                    null
                }
            }
        setState { copy(pagingDataFlow = pagingDataFlow) }
        //pagingDataFlow.collectLatest { setState { copy(pagingData = it) } }
    }

    private fun subscribeToOnlineStatus(uid: String) {
        val flow = userUseCases.addUserStatusListener(uid, viewModelScope)
        viewModelScope.launch(dispatcher) {
            flow.collect {
                val typing = it.first
                val online = it.second
                val lastSeenTime = it.third
                val status = if (typing) TYPING_STATUS
                else if (online) ONLINE_STATUS else timeToString(lastSeenTime)
                setState { copy(onlineStatus = status) }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeToString(lastTimeOnline: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTimeOnline))
    }
}

private fun insertDateSeparators(message1: MessageViewData?, message2: MessageViewData?, locale: Locale) {
    if (message1 != null && message2 != null) {
        val day1 = GregorianCalendar().apply { time = Date(message1.message.time) }
            .get(Calendar.DAY_OF_YEAR)
        val day2 = GregorianCalendar().apply { time = Date(message2.message.time) }
            .get(Calendar.DAY_OF_YEAR)
        if (day1 != day2)
            message1.dateDelimiter =
                SimpleDateFormat("dd MMMM yyyy", locale).format(Date(message1.message.time))
    }
}