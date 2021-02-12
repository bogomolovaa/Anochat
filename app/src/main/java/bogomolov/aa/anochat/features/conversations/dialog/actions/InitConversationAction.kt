package bogomolov.aa.anochat.features.conversations.dialog.actions

import android.annotation.SuppressLint
import android.util.Log
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.conversations.dialog.ConversationActionContext
import bogomolov.aa.anochat.features.conversations.dialog.ConversationViewModel
import bogomolov.aa.anochat.features.conversations.dialog.MessageView
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ONLINE_STATUS = "online"

class InitConversationAction(val conversationId: Long) : UserAction<ConversationActionContext> {
    private lateinit var viewModel: ConversationViewModel
    private lateinit var repository: Repository
    private lateinit var context: ConversationActionContext

    override suspend fun execute(context: ConversationActionContext) {
        this.context = context
        this.viewModel = context.viewModel
        repository = context.repository
        initConversation()
    }

    private suspend fun initConversation() {
        Log.i("InitConversationAction", "initConversation conversationId $conversationId")
        val conversation = repository.getConversation(conversationId)
        viewModel.setState { copy(conversation = conversation) }
        val pagedListLiveData = loadMessages(conversation)
        viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
        subscribeToOnlineStatus(conversation.user.uid)
    }

    private fun subscribeToOnlineStatus(uid: String) {
        val flow = repository.addUserStatusListener(uid, viewModel.viewModelScope)
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            flow.collect {
                val online = it.first
                val lastSeenTime = it.second
                val status = if (online) ONLINE_STATUS else timeToString(lastSeenTime)
                viewModel.setState { copy(onlineStatus = status) }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeToString(lastTimeOnline: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTimeOnline))
    }

    //todo: Day delimiters
    @SuppressLint("SimpleDateFormat")
    private fun loadMessages(conversation: Conversation) = LivePagedListBuilder(
        repository.loadMessagesDataSource(conversation.id, viewModel.viewModelScope).mapByPage {
            val list: MutableList<MessageView> = ArrayList()
            if (it != null) {
                var lastDay = -1
                for ((i, message) in it.listIterator().withIndex()) {
                    val messageView = MessageView(message)
                    val day = GregorianCalendar().apply { time = Date(message.time) }
                        .get(Calendar.DAY_OF_YEAR)
                    if (i > 0) {
                        if (lastDay != day) {
                            val locale =
                                ConfigurationCompat.getLocales(context.appContext.resources.configuration)[0]
                            val dateString =
                                SimpleDateFormat(
                                    "dd MMMM yyyy",
                                    locale
                                ).format(Date(message.time))
                            messageView.dateDelimiter = dateString
                        }
                    }
                    lastDay = day
                    list.add(messageView)
                }
            }
            list
        }, 10
    ).build()
}