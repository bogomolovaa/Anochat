package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.getFilePath
import bogomolov.aa.anochat.features.shared.getRandomFileName
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val ONLINE_STATUS = "online"
private const val TAG = "DialogUiState"

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
    val pagedListLiveData: LiveData<PagedList<MessageView>>? = null,
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
class StartRecordingAction() : UserAction
class StopRecordingAction : UserAction
class StartPlayingAction(val audioFile: String? = null, val messageId: String? = null) : UserAction
class PausePlayingAction : UserAction

@SuppressLint("StaticFieldLeak")
class ConversationViewModel @Inject constructor(
    private val context: Context,
    private val userUseCases: UserUseCases,
    private val conversationUseCases: ConversationUseCases,
    private val messageUseCases: MessageUseCases
) : BaseViewModel<DialogUiState>() {
    private var recordingJob: Job? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playJob: Job? = null
    private var startTime = 0L
    private var tempElapsed = 0L

    override fun createInitialState() = DialogUiState()

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(dispatcher) {
            conversationUseCases.deleteConversationIfNoMessages(state.conversation!!)
        }
    }

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
        var playingState = state.playingState
        if (playingState == null && audioFile != null) {
            val duration = initPlayer(audioFile)
            playingState =
                PlayingState(audioFile = audioFile, duration = duration, messageId = messageId)
        }
        if (mediaPlayer != null) {
            startPlay()
            playingState = playingState?.copy(paused = false)
            setState { copy(playingState = playingState) }
        }
    }

    private fun initPlayer(audioFile: String): Long {
        val player = MediaPlayer()
        val filePath = getFilePath(context, audioFile)
        if (File(filePath).exists()) {
            player.setDataSource(filePath)
            player.prepare()
            val duration = player.duration.toLong()
            player.setOnCompletionListener {
                playJob?.cancel()
                setStateAsync { copy(playingState = null) }
                mediaPlayer = null
            }
            mediaPlayer = player
            return duration
        } else {
            Log.w(TAG, "error file: $filePath not exist")
        }
        return 0
    }

    private suspend fun startPlay() {
        mediaPlayer?.start()
        startTime = System.currentTimeMillis()
        playJob = viewModelScope.launch(dispatcher) {
            while (true) {
                delay(1000)
                val time = System.currentTimeMillis() - startTime + tempElapsed
                val playingState = state.playingState?.copy(elapsed = time)
                if (playingState != null) setState { copy(playingState = playingState) }
            }
        }
    }

    private suspend fun PausePlayingAction.execute() {
        val player = mediaPlayer
        if (player != null) {
            tempElapsed += System.currentTimeMillis() - startTime
            playJob?.cancel()
            player.pause()
            val playingState = state.playingState?.copy(paused = true)
            setState { copy(playingState = playingState) }
        }
    }

    private suspend fun StartRecordingAction.execute() {
        val audioFile = "${getRandomFileName()}.3gp"
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setOutputFile(getFilePath(context, audioFile))
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.prepare()
        recorder.start()
        mediaRecorder = recorder
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
        val recorder = mediaRecorder
        if (recorder != null) {
            recorder.stop()
            recorder.reset()
            recorder.release()
        }
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
                replyMessage = if (replyId != null) Message(messageId = replyId) else null,
                audio = audio,
                image = image
            )
            setState {
                copy(
                    inputState = InputStates.INITIAL,
                    text = "",
                    replyMessage = null,
                    photoPath = null,
                    audioFile = null
                )
            }
            messageUseCases.sendMessage(message, conversation.user.uid)
        }
    }

    private fun DeleteMessagesAction.execute() {
        messageUseCases.deleteMessages(ids)
    }

    private suspend fun InitConversationAction.execute() {
        val conversation = conversationUseCases.getConversation(conversationId)
        setState { copy(conversation = conversation) }
        val pagedListLiveData = loadMessages(conversation)
        setState { copy(pagedListLiveData = pagedListLiveData) }
        subscribeToOnlineStatus(conversation.user.uid)
    }

    private fun InitConversationAction.loadMessages(conversation: Conversation) =
        LivePagedListBuilder(
            messageUseCases.loadMessagesDataSource(conversation.id)
                .mapByPage {
                    viewModelScope.launch(dispatcher) {
                        messageUseCases.notifyAsViewed(it)
                    }
                    it
                }
                .mapByPage(toMessageView), 10
        ).build()

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