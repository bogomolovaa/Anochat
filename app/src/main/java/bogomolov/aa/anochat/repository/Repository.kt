package bogomolov.aa.anochat.repository

import android.content.Context
import androidx.paging.DataSource
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User

interface Repository : IFirebaseRepository{
    suspend fun getConversation(id: Long): Conversation

    suspend fun saveMessage(message: Message, conversationId: Long)

    suspend fun saveAndSendMessage(message: Message, conversation: Conversation)

    fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message>

    fun loadConversations(): DataSource.Factory<Int, Conversation>

    suspend fun getConversation(user: User): Long

    suspend fun receiveMessage(text: String?, uid: String?, image: String?):Message?

    fun getContext(): Context

}