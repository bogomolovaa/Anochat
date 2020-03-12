package bogomolov.aa.anochat.repository

import android.content.Context
import androidx.paging.DataSource
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User

interface Repository : IFirebaseRepository{

    suspend fun sendMessage(message: Message)

    suspend fun saveMessage(message: Message, conversationId: Long)

    suspend fun saveAndSendMessage(message: Message, conversation: Conversation)

    fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message>

    suspend fun findUser(uid: String): User?

    suspend fun updateUserTo(user: User)

    suspend fun updateUserFrom(user: User)

    fun loadConversations(): DataSource.Factory<Int, Conversation>

    suspend fun getConversation(id: Long): Conversation

    suspend fun getConversation(user: User): Long

    suspend fun receiveMessage(text: String?, uid: String, messageId: String, replyId: String?, image: String?, audio: String?):Message?

    suspend fun reportAsViewed(conversationId: Long)

    fun getContext(): Context

    fun receiveReport(messageId: String, received: Int, viewed: Int)


}