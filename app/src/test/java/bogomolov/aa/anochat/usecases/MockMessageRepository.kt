package bogomolov.aa.anochat.usecases

import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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

    override suspend fun sendMessage(message: Message, uid: String) {
        remoteUseCases.receiveMessage(message, myUid)
    }

    override suspend fun notifyAsReceived(messageId: String, uid: String) {
        remoteUseCases.receiveReport(messageId, 1, 0)
    }

    override suspend fun getMessage(messageId: String): Message? {
        val message = this.message
        return if (message != null && message.messageId == messageId) message else null
    }

    override suspend fun receiveReport(messageId: String, received: Int, viewed: Int) {
        getMessage(messageId)?.let {
            this.message = it.copy(received = received, viewed = viewed)
        }
    }

    override suspend fun notifyAsNotReceived(messageId: String, uid: String) {
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
        return flowOf()
    }

    override fun searchMessagesDataSource(search: String): Flow<PagingData<Conversation>> {
        return flowOf()
    }

    override suspend fun deleteMessages(ids: Set<Long>) {
    }

    override suspend fun notifyAsViewed(message: Message, uid: String) {
    }

    override suspend fun startTypingTo(uid: String) {
    }

    override suspend fun stopTypingTo(uid: String) {
    }
}