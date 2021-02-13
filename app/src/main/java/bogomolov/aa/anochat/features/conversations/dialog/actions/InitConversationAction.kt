package bogomolov.aa.anochat.features.conversations.dialog.actions

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.features.conversations.dialog.DialogUiState
import bogomolov.aa.anochat.features.conversations.dialog.MessageView
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.DefaultUserAction
import bogomolov.aa.anochat.features.shared.mvi.RepositoryBaseViewModel
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val ONLINE_STATUS = "online"

class InitConversationAction(
    private val conversationId: Long,
    private val toMessageView: (List<Message>) -> List<MessageView>
) : DefaultUserAction<DialogUiState>() {
    private lateinit var viewModel: RepositoryBaseViewModel<DialogUiState>
    private lateinit var repository: Repository

    override suspend fun execute(context: DefaultContext<DialogUiState>) {
        this.viewModel = context.viewModel
        repository = context.repository
        initConversation()
    }

    private suspend fun initConversation() {
        val conversation = repository.getConversation(conversationId)
        viewModel.setState { copy(conversation = conversation) }
        val pagedListLiveData = loadMessages(conversation)
        viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
        subscribeToOnlineStatus(conversation.user.uid)
    }

    private fun loadMessages(conversation: Conversation) = LivePagedListBuilder(
        repository.loadMessagesDataSource(conversation.id, viewModel.viewModelScope)
            .mapByPage(toMessageView), 10
    ).build()

    private fun subscribeToOnlineStatus(uid: String) {
        val flow = repository.addUserStatusListener(uid, viewModel.viewModelScope)
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            flow.collect {
                val online = it.first
                val lastSeenTime = it.second
                val status = if (online) ONLINE_STATUS else timeToString(lastSeenTime)
                viewModel.setState { copy(onlineStatus = status) }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeToString(lastTimeOnline: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTimeOnline))
    }
}