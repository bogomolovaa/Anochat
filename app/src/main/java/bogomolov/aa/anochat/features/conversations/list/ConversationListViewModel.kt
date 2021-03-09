package bogomolov.aa.anochat.features.conversations.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

class ConversationsUiState : UiState

class InitConversationsAction : UserAction
class DeleteConversationsAction(val ids: Set<Long>) : UserAction
class SignOutAction : UserAction

@HiltViewModel
class ConversationListViewModel
@Inject constructor(
    private val conversationUseCases: ConversationUseCases,
    private val authRepository: AuthRepository
) : BaseViewModel<ConversationsUiState>() {
    private val _conversationsLiveData = MediatorLiveData<PagedList<Conversation>>()
    val conversationsLiveData: LiveData<PagedList<Conversation>>
        get() = _conversationsLiveData

    override fun createInitialState() = ConversationsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is InitConversationsAction) action.execute()
        if (action is DeleteConversationsAction) action.execute()
        if (action is SignOutAction) action.execute()
    }

    private fun InitConversationsAction.execute() {
        val liveData =
            LivePagedListBuilder(conversationUseCases.loadConversationsDataSource(), 10).build()
        _conversationsLiveData.addSource(liveData){
            _conversationsLiveData.value = it
        }
    }

    private fun DeleteConversationsAction.execute() {
        conversationUseCases.deleteConversations(HashSet(ids))
    }

    private fun SignOutAction.execute() {
        authRepository.signOut()
    }
}