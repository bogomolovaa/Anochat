package bogomolov.aa.anochat.viewmodel

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConversationViewModel
@Inject constructor(val repository: Repository) : ViewModel() {
    val pagedListLiveData = MediatorLiveData<PagedList<Message>>()
    var conversationId = 0L

    fun loadMessages(conversationId: Long) {
        this.conversationId = conversationId
        viewModelScope.launch(Dispatchers.IO) {
            val dataSource = repository.loadMessages(conversationId)
            val liveData = LivePagedListBuilder(dataSource, 10).build()
            launch(Dispatchers.Main) {
                pagedListLiveData.addSource(liveData) {
                    pagedListLiveData.value = it
                }
            }

        }
    }

    fun onNewMessage(messageText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val message = Message(
                text = messageText,
                time = System.currentTimeMillis(),
                conversationId = conversationId
            )
            repository.addMessage(message)
        }
    }
}