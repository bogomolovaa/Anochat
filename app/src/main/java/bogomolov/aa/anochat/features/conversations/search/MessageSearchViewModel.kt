package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessageSearchUiState : UiState

class MessageSearchAction(val query: String) : UserAction

@HiltViewModel
class MessageSearchViewModel
@Inject constructor(private val messageUseCases: MessageUseCases) :
    BaseViewModel<MessageSearchUiState>() {
    private val _messagesLiveData = MediatorLiveData<PagingData<Conversation>>()
    val messagesLiveData: LiveData<PagingData<Conversation>>
        get() = _messagesLiveData

    override fun createInitialState() = MessageSearchUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is MessageSearchAction) action.execute()
    }

    private suspend fun MessageSearchAction.execute() {
        //todo: paging
        messageUseCases.searchMessagesDataSource(query).cachedIn(viewModelScope).collect {
            _messagesLiveData.postValue(it)
        }
    }
}