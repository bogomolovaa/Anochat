package bogomolov.aa.anochat.viewmodel

import android.util.Log
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.repository.Repository
import bogomolov.aa.anochat.view.MessageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class ConversationViewModel
@Inject constructor(val repository: Repository) : ViewModel() {
    var conversationLiveData = MutableLiveData<Conversation>()

    fun loadMessages(conversationId: Long): LiveData<PagedList<MessageView>> {
        Log.i("test", "load messages conversationId ${conversationId}")
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = repository.getConversation(conversationId)
            if (conversation.lastMessage?.isMine() == false)
                repository.reportAsViewed(conversationId)
            conversationLiveData.postValue(conversation)
        }
        return LivePagedListBuilder(repository.loadMessages(conversationId).mapByPage {
            val list: MutableList<MessageView> = ArrayList()
            if (it != null) {
                var lastDay = -1
                for ((i, message) in it.listIterator().withIndex()) {
                    val messageView = MessageView(message)
                    val day = GregorianCalendar().apply { time = Date(message.time) }
                        .get(Calendar.DAY_OF_YEAR)
                    if (i > 0) {
                        if (lastDay != day) {
                            val dateString = SimpleDateFormat(
                                "dd MMMM yyyy",
                                ConfigurationCompat.getLocales(repository.getContext().resources.configuration)[0]
                            ).format(Date(message.time))
                            messageView.dateDelimiter = dateString
                        }
                    }
                    lastDay = day
                    list.add(messageView)
                }
            }
            list
        }, 10).build()
    }


    fun sendMessage(messageText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = conversationLiveData.value
            if (conversation != null) {
                val message = Message(
                    text = messageText,
                    time = System.currentTimeMillis(),
                    conversationId = conversation.id
                )
                repository.saveAndSendMessage(message, conversation)
            }
        }
    }


}