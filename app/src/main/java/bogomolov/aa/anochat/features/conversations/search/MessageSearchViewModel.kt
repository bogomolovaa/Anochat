package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

data class MessageSearchUiState(
    val pagingData: PagingData<Conversation>? = null
)

@HiltViewModel
class MessageSearchViewModel @Inject constructor(
    private val messageUseCases: MessageUseCases
) : BaseViewModel<MessageSearchUiState>(MessageSearchUiState()) {

    fun messageSearch(query: String) = execute {
        val pagingData =
            messageUseCases.searchMessagesDataSource(query)
                .cachedIn(viewModelScope.plus(dispatcher)).first()
        setState { copy(pagingData = pagingData) }
    }
}