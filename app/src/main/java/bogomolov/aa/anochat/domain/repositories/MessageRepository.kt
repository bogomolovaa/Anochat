package bogomolov.aa.anochat.domain.repositories

import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository : MessageUseCasesInRepository {
    suspend fun saveMessage(message: Message): Long
    suspend fun getPendingMessages(uid: String): List<Message>
    suspend fun getMessage(messageId: String): Message?
    suspend fun sendMessage(message: Message, uid: String): String
    suspend fun sendAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray
    ): Boolean

    suspend fun receiveAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray
    )

    suspend fun notifyAsReceived(messageId: String)
    suspend fun notifyAsNotReceived(messageId: String)
    suspend fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean)
    fun loadMessagesDataSource(conversationId: Long): Flow<PagingData<Message>>

}

interface MessageUseCasesInRepository {
    fun searchMessagesDataSource(search: String): Flow<PagingData<Conversation>>

    suspend fun deleteMessages(ids: Set<Long>)
    suspend fun receiveReport(messageId: String, received: Int, viewed: Int)
    suspend fun notifyAsViewed(message: Message)

    suspend fun startTypingTo(uid: String)
    suspend fun stopTypingTo(uid: String)
}