package bogomolov.aa.anochat.features.conversations.list

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.shared.mvi.*
import bogomolov.aa.anochat.repository.repositories.Repository
import javax.inject.Inject

data class ConversationsUiState(
    val pagedListLiveData: LiveData<PagedList<Conversation>>? = null
) : UiState

class InitConversationsAction : UserAction
class DeleteConversationsAction(val ids: Set<Long>) : UserAction
class SignOutAction : UserAction

class ConversationListViewModel
@Inject constructor(private val repository: Repository) : BaseViewModel<ConversationsUiState>() {

    override fun createInitialState() = ConversationsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is InitConversationsAction) action.execute()
        if (action is DeleteConversationsAction) action.execute()
        if (action is SignOutAction) action.execute()
    }

    private suspend fun InitConversationsAction.execute() {
        val pagedListLiveData =
            LivePagedListBuilder(repository.conversationRepository.loadConversationsDataSource(), 10).build()
        setState { copy(pagedListLiveData = pagedListLiveData) }
    }

    private fun DeleteConversationsAction.execute() {
        repository.conversationRepository.deleteConversations(HashSet(ids))
    }

    private fun SignOutAction.execute() {
        repository.authRepository.signOut()
    }
}