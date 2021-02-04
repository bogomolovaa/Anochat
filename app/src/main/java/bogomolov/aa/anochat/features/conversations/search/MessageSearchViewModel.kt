package bogomolov.aa.anochat.features.conversations.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

class MessageSearchViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {

    fun search(searchString: String): LiveData<PagedList<Conversation>> =
        LivePagedListBuilder(repository.searchMessagesDataSource(searchString), 10).build()


}