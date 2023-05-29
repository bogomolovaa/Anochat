package bogomolov.aa.anochat.usecases

import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class MockMessageRepository(
    private val myUid: String,
    private val getAttachment: () -> ByteArray,
) : MessageRepository {
    lateinit var remoteUseCases: MessageUseCases
    private var message: Message? = null
    var attachment = getAttachment()

    override suspend fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean) {
        if (initiator) {
            remoteUseCases.receivedPublicKey(publicKey, myUid)
        } else {
            remoteUseCases.finallyReceivedPublicKey(publicKey, myUid)
        }
    }

    override suspend fun getPendingMessages(uid: String): List<Message> {
        val message = this.message
        return if (message != null && message.sent == 0) listOf(message) else listOf()
    }

    override suspend fun saveMessage(message: Message): Long {
        this.message = message
        return 1
    }

    override suspend fun sendMessage(message: Message, uid: String): String {
        remoteUseCases.receiveMessage(message, myUid) {}
        return message.messageId
    }

    override suspend fun notifyAsReceived(messageId: String) {
        remoteUseCases.receiveReport(messageId, 1, 0)
    }

    override suspend fun getMessage(messageId: String): Message? {
        val message = this.message
        return if (message != null && message.messageId == messageId) message else null
    }

    override suspend fun receiveReport(messageId: String, received: Int, viewed: Int) {
        val message = getMessage(messageId)
        if (message != null) {
            message.received = received
            message.viewed = viewed
        }
    }

    override suspend fun notifyAsNotReceived(messageId: String) {
        println("message $messageId notifyAsNotReceived")
    }

    override suspend fun sendAttachment(
        message: Message,
        uid: String,
        convert: (ByteArray) -> ByteArray
    ): Boolean {
        attachment = convert(attachment)
        return true
    }

    override suspend fun receiveAttachment(
        message: Message,
        uid: String,
        convert: (ByteArray) -> ByteArray
    ) {
        attachment = convert(getAttachment())
    }


    override fun loadMessagesDataSource(conversationId: Long): Flow<PagingData<Message>> {
        TODO("Not yet implemented")
    }

    override fun searchMessagesDataSource(search: String): Flow<PagingData<Conversation>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMessages(ids: Set<Long>) {
        TODO("Not yet implemented")
    }

    override suspend fun notifyAsViewed(message: Message) {
        TODO("Not yet implemented")
    }

    override suspend fun startTypingTo(uid: String) {
        TODO("Not yet implemented")
    }

    override suspend fun stopTypingTo(uid: String) {
        TODO("Not yet implemented")
    }
}