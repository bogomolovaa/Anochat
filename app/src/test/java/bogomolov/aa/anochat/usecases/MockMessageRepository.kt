package bogomolov.aa.anochat.usecases

import androidx.paging.DataSource
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import kotlinx.coroutines.runBlocking

class MockMessageRepository(
    private val myUid: String,
    private val getAttachment: () -> ByteArray,
) : MessageRepository {
    lateinit var remoteUseCases: MessageUseCases
    private var message: Message? = null
    var attachment = getAttachment()

    override fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean) {
        if (initiator) {
            remoteUseCases.receivedPublicKey(publicKey, myUid)
        } else {
            remoteUseCases.finallyReceivedPublicKey(publicKey, myUid)
        }
    }

    override fun getPendingMessages(uid: String): List<Message> {
        val message = this.message
        return if (message != null && message.sent == 0) listOf(message) else listOf()
    }

    override fun saveMessage(message: Message): Long {
        this.message = message
        return 1
    }

    override fun sendMessage(message: Message, uid: String): String {
        runBlocking {
            remoteUseCases.receiveMessage(message, myUid) {}
        }
        return message.messageId
    }

    override fun notifyAsReceived(messageId: String) {
        remoteUseCases.receiveReport(messageId, 1, 0)
    }

    override fun getMessage(messageId: String): Message? {
        val message = this.message
        return if (message != null && message.messageId == messageId) message else null
    }

    override fun receiveReport(messageId: String, received: Int, viewed: Int) {
        val message = getMessage(messageId)
        if (message != null) {
            message.received = received
            message.viewed = viewed
        }
    }

    override fun notifyAsNotReceived(messageId: String) {
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
    ): Boolean {
        attachment = convert(getAttachment())
        return true
    }


    override fun searchMessagesDataSource(search: String): DataSource.Factory<Int, Conversation> {
        TODO("Not yet implemented")
    }

    override fun loadMessagesDataSource(conversationId: Long): DataSource.Factory<Int, Message> {
        TODO("Not yet implemented")
    }

    override fun deleteMessages(ids: Set<Long>) {
        TODO("Not yet implemented")
    }

    override fun notifyAsViewed(message: Message) {
        TODO("Not yet implemented")
    }
}