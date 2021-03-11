package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.AudioPlayer
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

const val ONLINE_STATUS = "online"

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
    var recyclerViewState: Parcelable? = null,
    val inputState: InputStates = InputStates.INITIAL,
    val replyMessage: Message? = null,
    val audioFile: String? = null,
    val photoPath: String? = null,
    val text: String = "",
    val audioLengthText: String = "",
    val playingState: PlayingState? = null
) : UiState

data class PlayingState(
    val audioFile: String,
    val duration: Long = 0,
    val elapsed: Long = 0,
    val messageId: String? = null,
    val paused: Boolean = false
)

class SendMessageAction(
    val text: String? = null,
    val audio: String? = null,
    val image: String? = null
) : UserAction

class InitConversationAction(
    val conversationId: Long,
    val toMessageView: (List<Message>) -> List<MessageView>
) : UserAction

class DeleteMessagesAction(val ids: Set<Long>) : UserAction
class StartRecordingAction : UserAction
class StopRecordingAction : UserAction
class StartPlayingAction(val audioFile: String? = null, val messageId: String? = null) : UserAction
class PausePlayingAction : UserAction

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val conversationUseCases: ConversationUseCases,
    private val messageUseCases: MessageUseCases,
    private val audioPlayer: AudioPlayer
) : BaseViewModel<DialogUiState>() {
    private var recordingJob: Job? = null
    private var playJob: Job? = null
    private var startTime = 0L
    private var tempElapsed = 0L
    private var conversationInitialized = false
    private val _messagesLiveData = MediatorLiveData<PagedList<MessageView>>()
    val messagesLiveData: LiveData<PagedList<MessageView>>
        get() = _messagesLiveData

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(dispatcher) {
            conversationUseCases.deleteConversationIfNoMessages(state.conversation!!)
        }
    }

    override fun createInitialState() = DialogUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is SendMessageAction) action.execute()
        if (action is InitConversationAction) action.execute()
        if (action is DeleteMessagesAction) action.execute()
        if (action is StartRecordingAction) action.execute()
        if (action is StopRecordingAction) action.execute()
        if (action is StartPlayingAction) action.execute()
        if (action is PausePlayingAction) action.execute()
    }

    private suspend fun StartPlayingAction.execute() {
        if (state.playingState == null) init()
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

    private suspend fun StartPlayingAction.init() {
        if (audioFile != null) {
            val duration = audioPlayer.initPlayer(audioFile) {
                playJob?.cancel()
                setStateAsync { copy(playingState = null) }
            }
            val newPlayingState =
                PlayingState(audioFile = audioFile, duration = duration, messageId = messageId)
            setState { copy(playingState = newPlayingState) }
        }
    }

    private suspend fun PausePlayingAction.execute() {
        if (audioPlayer.pausePlay()) {
            tempElapsed += System.currentTimeMillis() - startTime
            playJob?.cancel()
            val playingState = state.playingState?.copy(paused = true)
            setState { copy(playingState = playingState) }
        }
    }

    private suspend fun StartRecordingAction.execute() {
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

    private suspend fun StopRecordingAction.execute() {
        audioPlayer.stopRecording()
        recordingJob?.cancel()
        setState { copy(inputState = InputStates.VOICE_RECORDED) }
    }

    private suspend fun SendMessageAction.execute() {
        val conversation = state.conversation
        if (conversation != null) {
            val replyId = state.replyMessage?.messageId
            val message = Message(
                text = text ?: "",
                time = System.currentTimeMillis(),
                isMine = true,
                conversationId = conversation.id,
                replyMessageId = replyId,
                audio = audio,
                image = image
            )
            setState {
                copy(
                    inputState = InputStates.INITIAL,
                    text = "",
                    replyMessage = null,
                    photoPath = null,
                    audioFile = null,
                    recyclerViewState = null
                )
            }
            messageUseCases.sendMessage(message, conversation.user.uid)
        }
    }

    private fun DeleteMessagesAction.execute() {
        messageUseCases.deleteMessages(ids)
    }

    private suspend fun InitConversationAction.execute() {
        if (!conversationInitialized) {
            conversationInitialized = true
            val conversation = conversationUseCases.getConversation(conversationId)
            setState { copy(conversation = conversation) }
            loadMessages(conversation)
            subscribeToOnlineStatus(conversation.user.uid)
        }
    }

    private fun InitConversationAction.loadMessages(conversation: Conversation) {
        val liveData = LivePagedListBuilder(
            messageUseCases.loadMessagesDataSource(conversation.id, viewModelScope)
                .mapByPage(toMessageView),
            10
        ).build()
        _messagesLiveData.addSource(liveData) { _messagesLiveData.postValue(it) }
    }

    private fun subscribeToOnlineStatus(uid: String) {
        val flow = userUseCases.addUserStatusListener(uid, viewModelScope)
        viewModelScope.launch(dispatcher) {
            flow.collect {
                val online = it.first
                val lastSeenTime = it.second
                val status = if (online) ONLINE_STATUS else timeToString(lastSeenTime)
                setState { copy(onlineStatus = status) }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeToString(lastTimeOnline: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTimeOnline))
    }
}