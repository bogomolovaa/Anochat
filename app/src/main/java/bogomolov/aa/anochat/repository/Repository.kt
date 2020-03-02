package bogomolov.aa.anochat.repository

import androidx.paging.DataSource
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User

interface Repository : IFirebaseRepository{
    suspend fun getConversation(id: Long): Conversation

    suspend fun sendMessage(message: Message, conversation: Conversation)

    fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message>

    fun loadConversations(): DataSource.Factory<Int, Conversation>

    suspend fun getConversation(user: User): Long

    suspend fun receiveMessage(text: String?, uid: String?):Message?

}