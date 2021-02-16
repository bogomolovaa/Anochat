package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val ONLINE_STATUS = "online"

data class DialogUiState(
    val conversation: Conversation? = null,
    val onlineStatus: String = "",
    val pagedListLiveData: LiveData<PagedList<MessageView>>? = null,
    var recyclerViewState: Parcelable? = null
) : UiState

class SendMessageAction(
    val text: String? = null,
    val replyId: String? = null,
    val audio: String? = null,
    val image: String? = null
) : UserAction

class InitConversationAction(
    val conversationId: Long,
    val toMessageView: (List<Message>) -> List<MessageView>
) : UserAction

class DeleteMessagesAction(val ids: Set<Long>) : UserAction

class ConversationViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val conversationUseCases: ConversationUseCases,
    private val messageUseCases: MessageUseCases
) :
    BaseViewModel<DialogUiState>() {

    override fun createInitialState() = DialogUiState()

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(dispatcher) {
            conversationUseCases.deleteConversationIfNoMessages(currentState.conversation!!)
        }
    }

    override suspend fun handleAction(action: UserAction) {
        if (action is SendMessageAction) action.execute()
        if (action is InitConversationAction) action.execute()
        if (action is DeleteMessagesAction) action.execute()
    }

    private suspend fun SendMessageAction.execute() {
        val conversation = currentState.conversation
        if (conversation != null) {
            val message = Message(
                text = text ?: "",
                time = System.currentTimeMillis(),
                isMine = true,
                conversationId = conversation.id,
                replyMessage = if (replyId != null) Message(messageId = replyId) else null,
                audio = audio,
                image = image
            )
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