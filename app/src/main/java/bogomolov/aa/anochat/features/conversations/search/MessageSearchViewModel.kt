package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class MessageSearchUiState(
    val pagedListLiveData: LiveData<PagedList<Conversation>>? = null
) : UiState

class MessageSearchAction(val query: String) : UserAction

@HiltViewModel
class MessageSearchViewModel
@Inject constructor(private val messageUseCases: MessageUseCases) : BaseViewModel<MessageSearchUiState>() {

    override fun createInitialState() = MessageSearchUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is MessageSearchAction) action.execute()
    }

    private suspend fun MessageSearchAction.execute() {
        val pagedListLiveData =
            LivePagedListBuilder(messageUseCases.searchMessagesDataSource(query), 10).build()
        setState { copy(pagedListLiveData = pagedListLiveData) }
    }
}