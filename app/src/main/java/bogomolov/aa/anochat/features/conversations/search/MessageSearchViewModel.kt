package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessageSearchUiState : UiState

class MessageSearchAction(val query: String) : UserAction

@HiltViewModel
class MessageSearchViewModel
@Inject constructor(private val messageUseCases: MessageUseCases) :
    BaseViewModel<MessageSearchUiState>() {
    private val _messagesLiveData = MediatorLiveData<PagedList<Conversation>>()
    val messagesLiveData: LiveData<PagedList<Conversation>>
        get() = _messagesLiveData

    override fun createInitialState() = MessageSearchUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is MessageSearchAction) action.execute()
    }

    private suspend fun MessageSearchAction.execute() {
        val liveData =
            LivePagedListBuilder(messageUseCases.searchMessagesDataSource(query), 10).build()
        withContext(Dispatchers.Main) {
            _messagesLiveData.addSource(liveData) { _messagesLiveData.value = it }
        }
    }
}