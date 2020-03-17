package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

class MessageSearchViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {

    fun search(searchString: String): LiveData<PagedList<Conversation>> =
        LivePagedListBuilder(repository.searchMessagesDataSource(searchString), 10).build()


}