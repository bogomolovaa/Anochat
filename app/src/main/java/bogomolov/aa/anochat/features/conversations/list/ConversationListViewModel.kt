package bogomolov.aa.anochat.features.conversations.list

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

data class ConversationsUiState(
    val pagingData: PagingData<Conversation>? = null
)

@HiltViewModel
class ConversationListViewModel
@Inject constructor(
    private val conversationUseCases: ConversationUseCases,
    private val authRepository: AuthRepository
) : BaseViewModel<ConversationsUiState>(ConversationsUiState()) {

    init {
        initConversations()
    }

    private fun initConversations() {
        viewModelScope.launch(dispatcher) {
            conversationUseCases.loadConversationsDataSource()
                .cachedIn(viewModelScope.plus(dispatcher)).collect {
                    setState { copy(pagingData = it) }
                }
        }
    }

    fun deleteConversations(ids: Set<Long>) = execute {
        conversationUseCases.deleteConversations(HashSet(ids))
    }

    fun signOut() = execute {
        authRepository.signOut()
    }
}