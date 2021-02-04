package bogomolov.aa.anochat.features.conversations.list

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.HashSet


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