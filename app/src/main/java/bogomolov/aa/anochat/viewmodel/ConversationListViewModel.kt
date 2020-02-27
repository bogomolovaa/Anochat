package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject


class ConversationListViewModel
@Inject constructor(val repository: Repository) : ViewModel() {
    val pagedListLiveData = LivePagedListBuilder(repository.loadConversations(), 10).build()

}