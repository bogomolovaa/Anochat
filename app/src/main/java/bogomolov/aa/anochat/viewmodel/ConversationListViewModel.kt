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
import kotlin.collections.HashSet
import kotlin.math.abs


class ConversationListViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {

    fun loadConversations() =
        LivePagedListBuilder(repository.loadConversationsDataSource(), 10).build()

    fun deleteConversations(ids: Set<Long>){
        val saveIds = HashSet(ids)
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConversations(saveIds)
        }
    }

    fun signOut() {
        repository.signOut()
    }
}