package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.os.Parcelable
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
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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
    var recyclerViewState: Parcelable? = null,
    val inputState: InputStates = InputStates.INITIAL,
    val replyMessage: Message? = null,
    val audioFile: String? = null,
    val photoPath: String? = null,
    val text: String = "",
    val audioLengthText: String = "",
    val playingState: PlayingState? = null,
    val pagingData: PagingData<MessageView>? = null
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
    val image: String? = null,
    val video: String? = null
) : UserAction

class InitConversationAction(
    val conversationId: Long,
    val insertDateDelimiters: (MessageView?, MessageView?) -> Unit
) : UserAction

class TextChangedAction(
    val enteredText: String
) : UserAction

class DeleteMessagesAction(val ids: Set<Long>) : UserAction
class StartRecordingAction : UserAction
class StopRecordingAction : UserAction
class StartPlayingAction(val audioFile: String? = null, val messageId: String? = null) : UserAction
class PausePlayingAction : UserAction

data class NotifyAsViewed(val messages: List<MessageView>) : UserAction

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
    private var typingJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(dispatcher) {
            conversationUseCases.deleteConversationIfNoMessages(state.conversation?.id!!)
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
        if (action is TextChangedAction) action.execute()
        if (action is NotifyAsViewed) action.execute()

    }

    private fun NotifyAsViewed.execute() {
        messageUseCases.notifyAsViewed(messages.map { it.message })
    }

    private suspend fun TextChangedAction.execute() {
        val uid = state.conversation?.user?.uid
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
                image = image,
                video = video
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
            val conversation = conversationUseCases.getConversation(conversationId)!!
            setState { copy(conversation = conversation) }
            subscribeToMessages(conversation)
            subscribeToOnlineStatus(conversation.user.uid)
        }
    }

    private suspend fun InitConversationAction.subscribeToMessages(conversation: Conversation) {
        viewModelScope.launch(dispatcher) {
            messageUseCases.loadMessagesDataSource(conversation.id)
                .cachedIn(viewModelScope)
                .collect {
                    setState {
                        copy(
                            pagingData = it.map { MessageView(it) }.insertSeparators { m1, m2 ->
                                insertDateDelimiters(m1, m2)
                                null
                            }
                        )
                    }
                }
        }
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