package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.Comparator
import kotlin.math.abs


class ConversationListViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val pagedListLiveData = LivePagedListBuilder(
        repository.loadConversationsDataSource()
            .mapByPage {

                it
                //it.sortedByDescending { conversation -> conversation?.lastMessage?.time ?: 0 }
            }, 10
    ).build()

    fun signOut(){
        repository.signOut()
    }
}