package bogomolov.aa.anochat.domain.repositories

import androidx.paging.DataSource
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import java.io.File

interface MessageRepository : MessageUseCasesInRepository {
    fun saveMessage(message: Message)
    fun getPendingMessages(uid: String): List<Message>
    fun getMessage(messageId: String): Message?
    fun sendMessage(message: Message, uid: String)
    fun getAttachmentFile(fileName: String): File
    suspend fun sendAttachment(fileName: String, uid: String, byteArray: ByteArray)
    suspend fun receiveAttachment(fileName: String, uid: String, localFile: File)
    fun notifyAsReceived(messageId: String)
    fun notifyAsNotReceived(messageId: String)
    fun keyIsNotSentTo(uid: String): Boolean
    fun setKeyAsSentTo(uid: String)
    fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean)
}

interface MessageUseCasesInRepository {
    fun searchMessagesDataSource(search: String): DataSource.Factory<Int, Conversation>
    fun loadMessagesDataSource(conversationId: Long): DataSource.Factory<Int, Message>

    fun deleteMessages(ids: Set<Long>)
    fun receiveReport(messageId: String, received: Int, viewed: Int)
    fun notifyAsViewed(messages: List<Message>)
}