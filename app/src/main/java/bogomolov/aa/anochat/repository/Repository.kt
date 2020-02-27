package bogomolov.aa.anochat.repository

import androidx.paging.DataSource
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User

interface Repository : IFirebaseRepository{
    fun addMessage(message: Message)

    fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message>

    fun loadConversations(): DataSource.Factory<Int, Conversation>

    fun addConversation(conversation: Conversation)


}