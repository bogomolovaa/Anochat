package bogomolov.aa.anochat.features.conversations.list

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.shared.BaseViewModel
import bogomolov.aa.anochat.features.shared.UiState
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject
import kotlin.collections.HashSet

data class ConversationsUiState(
    val pagedListLiveData: LiveData<PagedList<Conversation>>? = null
) : UiState

class ConversationListViewModel
@Inject constructor(val repository: Repository) :
    BaseViewModel<ConversationsUiState, ConversationListViewModel>() {

    override fun createInitialState() = ConversationsUiState()
}

class InitConversationsAction() : UserAction<ConversationListViewModel> {

    override suspend fun execute(viewModel: ConversationListViewModel) {
        val pagedListLiveData =
            LivePagedListBuilder(viewModel.repository.loadConversationsDataSource(), 10).build()
        viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
    }
}

class DeleteConversationsAction(val ids: Set<Long>) : UserAction<ConversationListViewModel> {

    override suspend fun execute(viewModel: ConversationListViewModel) {
        viewModel.repository.deleteConversations(HashSet(ids))
    }
}

class SignOutAction() : UserAction<ConversationListViewModel> {

    override suspend fun execute(viewModel: ConversationListViewModel) {
        viewModel.repository.signOut()
    }
}