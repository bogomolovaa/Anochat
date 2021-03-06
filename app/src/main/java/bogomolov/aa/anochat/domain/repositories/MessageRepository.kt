package bogomolov.aa.anochat.domain.repositories

import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository : MessageUseCasesInRepository {
    fun saveMessage(message: Message): Long
    fun getPendingMessages(uid: String): List<Message>
    fun getMessage(messageId: String): Message?
    fun sendMessage(message: Message, uid: String): String
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

    fun notifyAsReceived(messageId: String)
    fun notifyAsNotReceived(messageId: String)
    fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean)
    fun loadMessagesDataSource(conversationId: Long): Flow<PagingData<Message>>

}

interface MessageUseCasesInRepository {
    fun searchMessagesDataSource(search: String): Flow<PagingData<Conversation>>

    fun deleteMessages(ids: Set<Long>)
    fun receiveReport(messageId: String, received: Int, viewed: Int)
    fun notifyAsViewed(message: Message)

    fun startTypingTo(uid: String)
    fun stopTypingTo(uid: String)
}