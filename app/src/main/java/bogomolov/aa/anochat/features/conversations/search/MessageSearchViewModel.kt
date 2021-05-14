package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class MessageSearchUiState(val pagingData: PagingData<Conversation>? = null) : UiState

class MessageSearchAction(val query: String) : UserAction

@HiltViewModel
class MessageSearchViewModel
@Inject constructor(private val messageUseCases: MessageUseCases) :
    BaseViewModel<MessageSearchUiState>() {

    override fun createInitialState() = MessageSearchUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is MessageSearchAction) action.execute()
    }

    private suspend fun MessageSearchAction.execute() {
        val pagingData =
            messageUseCases.searchMessagesDataSource(query).cachedIn(viewModelScope).first()
        setState { copy(pagingData = pagingData) }
    }
}