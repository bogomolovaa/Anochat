package bogomolov.aa.anochat.features.conversations.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(val pagingData: PagingData<Conversation>? = null) : UiState

class InitConversationsAction : UserAction
class DeleteConversationsAction(val ids: Set<Long>) : UserAction
class SignOutAction : UserAction

@HiltViewModel
class ConversationListViewModel
@Inject constructor(
    private val conversationUseCases: ConversationUseCases,
    private val authRepository: AuthRepository
) : BaseViewModel<ConversationsUiState>() {

    init {
        addAction(InitConversationsAction())
    }

    override fun createInitialState() = ConversationsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is InitConversationsAction) action.execute()
        if (action is DeleteConversationsAction) action.execute()
        if (action is SignOutAction) action.execute()
    }

    private suspend fun InitConversationsAction.execute() {
        viewModelScope.launch {
            conversationUseCases.loadConversationsDataSource().cachedIn(viewModelScope).collect {
                setState { copy(pagingData = it) }
            }
        }
    }

    private fun DeleteConversationsAction.execute() {
        conversationUseCases.deleteConversations(HashSet(ids))
    }

    private fun SignOutAction.execute() {
        authRepository.signOut()
    }
}