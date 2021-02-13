package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.RepositoryBaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

data class MessageSearchUiState(
    val pagedListLiveData: LiveData<PagedList<Conversation>>? = null
) : UiState

class MessageSearchViewModel
@Inject constructor(private val repository: Repository) :
    RepositoryBaseViewModel<MessageSearchUiState>(repository) {

    override fun createInitialState() = MessageSearchUiState()
}

class SearchMessages(val query: String) : UserAction<DefaultContext<MessageSearchUiState>> {

    override suspend fun execute(context: DefaultContext<MessageSearchUiState>) {
        val pagedListLiveData =
            LivePagedListBuilder(context.repository.searchMessagesDataSource(query), 10).build()
        context.viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
    }
}