package bogomolov.aa.anochat.viewmodel

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConversationViewModel
@Inject constructor(val repository: Repository) : ViewModel() {
    private val pagedListLiveData = MediatorLiveData<PagedList<Message>>()
    var conversation: Conversation? = null

    fun loadMessages_(conversationId: Long) {
        Log.i("test","load messages conversationId ${conversationId}")
        viewModelScope.launch(Dispatchers.IO) {
            conversation = repository.getConversation(conversationId)
            val dataSource = repository.loadMessages(conversationId)
            val liveData = LivePagedListBuilder(dataSource, 10).build()
            launch(Dispatchers.Main) {
                pagedListLiveData.addSource(liveData) {
                    pagedListLiveData.value = it
                }
            }

        }
    }

    fun loadMessages(conversationId: Long) : LiveData<PagedList<Message>>{
        Log.i("test","load messages conversationId ${conversationId}")
        viewModelScope.launch(Dispatchers.IO) {
            conversation = repository.getConversation(conversationId)
        }
        return LivePagedListBuilder(repository.loadMessages(conversationId),10).build()
    }

    fun clearMessages(){
        pagedListLiveData.postValue(null)
    }


    fun sendMessage(messageText: String) {
        val conversation = this.conversation!!
        viewModelScope.launch(Dispatchers.IO) {
            val message = Message(
                text = messageText,
                time = System.currentTimeMillis(),
                conversationId = conversation.id
            )
            repository.sendMessage(message, conversation)
        }
    }

}