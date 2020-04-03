package bogomolov.aa.anochat.viewmodel

import android.annotation.SuppressLint
import android.os.Parcelable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class ConversationViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val conversationLiveData = MutableLiveData<Conversation>()
    val onlineStatus = MutableLiveData<String>()
    private var removeStatusListener: (() -> Unit)? = null
    private var userOnline = false
    private var lastTimeOnline = 0L
    var recyclerViewState: Parcelable? = null

    override fun onCleared() {
        super.onCleared()
        removeStatusListener?.invoke()
    }

    fun deleteMessages(ids: Set<Long>) {
        val saveIds = HashSet(ids)
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessages(saveIds)
        }
    }

    fun deleteIfNoMessages() {
        if (conversationLiveData.value != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteConversationIfNoMessages(conversationLiveData.value!!.id)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun loadMessages(conversationId: Long): LiveData<PagedList<MessageView>> {
        if (conversationId != conversationLiveData.value?.id) recyclerViewState = null
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = repository.getConversation(conversationId)
            conversationLiveData.postValue(conversation)
            removeStatusListener =
                repository.addUserStatusListener(
                    uid = conversation.user.uid,
                    isOnline = { online ->
                        userOnline = online
                        if (online) {
                            onlineStatus.postValue("online")
                        } else {
                            if (lastTimeOnline > 0)
                                onlineStatus.postValue(
                                    SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTimeOnline))
                                )
                        }
                    },
                    lastTimeOnline = { lastTime ->
                        lastTimeOnline = lastTime
                        if (lastTime > 0 && !userOnline)
                            onlineStatus.postValue(
                                SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTime))
                            )
                    })


        }
        return LivePagedListBuilder(repository.loadMessages(conversationId).mapByPage {
            val list: MutableList<MessageView> = ArrayList()
            if (it != null) {
                var lastDay = -1
                for ((i, message) in it.listIterator().withIndex()) {
                    if (!message.isMine() && message.viewed == 0)
                        viewModelScope.launch(Dispatchers.IO) { repository.reportAsViewed(message) }
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


    fun sendMessage(messageText: String, replyMessageId: String?, audio: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = conversationLiveData.value
            if (conversation != null) {
                val message = Message(
                    text = messageText,
                    time = System.currentTimeMillis(),
                    conversationId = conversation.id,
                    replyMessage = if (replyMessageId != null) Message(messageId = replyMessageId) else null,
                    audio = audio
                )
                if (audio == null) {
                    repository.saveMessage(message, conversation.id)
                    repository.sendMessage(message)
                } else {
                    repository.saveMessage(message, conversation.id)
                    if (repository.uploadFile(audio, conversation.user.uid, true)) {
                        repository.sendMessage(message)
                    }
                }
            }
        }
    }


}