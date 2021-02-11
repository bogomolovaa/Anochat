package bogomolov.aa.anochat.features.conversations.list

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.shared.DefaultContext
import bogomolov.aa.anochat.features.shared.RepositoryBaseViewModel
import bogomolov.aa.anochat.features.shared.UiState
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

data class ConversationsUiState(
    val pagedListLiveData: LiveData<PagedList<Conversation>>? = null
) : UiState

class ConversationListViewModel
@Inject constructor(repository: Repository) :
    RepositoryBaseViewModel<ConversationsUiState>(repository) {

    override fun createInitialState() = ConversationsUiState()
}

class InitConversationsAction() : UserAction<DefaultContext<ConversationsUiState>> {

    override suspend fun execute(context: DefaultContext<ConversationsUiState>) {
        val pagedListLiveData =
            LivePagedListBuilder(context.repository.loadConversationsDataSource(), 10).build()
        context.viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
    }
}

class DeleteConversationsAction(val ids: Set<Long>) : UserAction<DefaultContext<ConversationsUiState>> {

    override suspend fun execute(context: DefaultContext<ConversationsUiState>) {
        context.repository.deleteConversations(HashSet(ids))
    }
}

class SignOutAction() : UserAction<DefaultContext<ConversationsUiState>> {

    override suspend fun execute(context: DefaultContext<ConversationsUiState>) {
        context.repository.signOut()
    }
}