package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

class MessageSearchViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    private val searchStringLiveData = MutableLiveData<String>()
    val searchLiveData: LiveData<PagedList<Conversation>> =
        Transformations.switchMap(searchStringLiveData) { searchString ->
            LivePagedListBuilder(repository.searchMessagesDataSource(searchString), 10).build()
        }

    fun search(searchString: String) {
        searchStringLiveData.postValue(searchString)
    }

}