package bogomolov.aa.anochat.features.conversations.dialog.actions

import android.annotation.SuppressLint
import android.util.Log
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.conversations.dialog.ConversationViewModel
import bogomolov.aa.anochat.features.conversations.dialog.MessageView
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ONLINE_STATUS = "online"

class InitConversationAction(val conversationId: Long) : UserAction<ConversationViewModel> {
    private var removeStatusListener: (() -> Unit)? = null
    private var userOnline = false
    private var lastTimeOnline = 0L
    private lateinit var viewModel: ConversationViewModel
    private lateinit var repository: Repository

    override suspend fun execute(viewModel: ConversationViewModel) {
        this.viewModel = viewModel
        repository = viewModel.repository
        initConversation()
    }

    private suspend fun initConversation() {
        val conversation = repository.getConversation(conversationId)
        viewModel.setState { copy(conversation = conversation) }
        val pagedListLiveData = loadMessages()
        viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
        subscribeToOnlineStatus(conversation)
    }

    private suspend fun subscribeToOnlineStatus(conversation: Conversation) {
        removeStatusListener =
            repository.addUserStatusListener(
                uid = conversation.user.uid,
                isOnline = { online ->
                    userOnline = online
                    if (online) {
                        viewModel.setStateAsync { copy(onlineStatus = ONLINE_STATUS) }
                    } else {
                        if (lastTimeOnline > 0) setLastSeen()
                    }
                },
                lastTimeOnline = { lastTime ->
                    lastTimeOnline = lastTime
                    if (lastTime > 0 && !userOnline) setLastSeen()
                })
    }

    @SuppressLint("SimpleDateFormat")
    private fun setLastSeen() {
        val time = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTimeOnline))
        viewModel.setStateAsync { copy(onlineStatus = time) }
    }

    @SuppressLint("SimpleDateFormat")
    private fun loadMessages() = LivePagedListBuilder(
        repository.loadMessages(viewModel.currentState.conversation!!.id).mapByPage {
            val list: MutableList<MessageView> = ArrayList()
            if (it != null) {
                var lastDay = -1
                for ((i, message) in it.listIterator().withIndex()) {
                    if (!message.isMine() && message.viewed == 0)
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            repository.reportAsViewed(
                                message
                            )
                        }
                    val messageView = MessageView(message)
                    val day = GregorianCalendar().apply { time = Date(message.time) }
                        .get(Calendar.DAY_OF_YEAR)
                    if (i > 0) {
                        if (lastDay != day) {
                            val locale =
                                ConfigurationCompat.getLocales(repository.getContext().resources.configuration)[0]
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


    suspend fun onDestroy() {
        if (viewModel.currentState.conversation != null)
            repository.deleteConversationIfNoMessages(viewModel.currentState.conversation!!.id)
        removeStatusListener?.invoke()
    }
}