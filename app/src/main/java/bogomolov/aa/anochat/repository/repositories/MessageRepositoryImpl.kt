package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
    private val fileStore: FileStore
) : MessageRepository {
    private val mapper = ModelEntityMapper()

    override fun searchMessagesDataSource(search: String) =
        db.messageDao().searchText("%$search%", getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    override fun loadMessagesDataSource(conversationId: Long) =
        db.messageDao().loadAll(conversationId).map { mapper.entityToModel(it)!! }

    override fun deleteMessages(ids: Set<Long>) {
        db.messageDao().deleteByIds(ids)
    }

    override fun receiveReport(messageId: String, received: Int, viewed: Int) {
        db.messageDao().updateReport(messageId, received, viewed)
    }

    override fun saveMessage(message: Message): Long {
        val entity = mapper.modelToEntity(message).apply { myUid = getMyUID()!! }
        return db.messageDao().insert(entity).also { id ->
            db.conversationDao().updateLastMessage(id, message.conversationId)
        }
    }

    override fun getPendingMessages(uid: String): List<Message> {
        val userId = db.userDao().findByUid(uid)?.id ?: return listOf()
        return mapper.entityToModel(db.messageDao().getNotSent(userId, getMyUID()!!))
    }

    override fun getMessage(messageId: String) =
        mapper.entityToModel(db.messageDao().getByMessageId(messageId))

    override fun sendMessage(message: Message, uid: String) =
        firebase.sendMessage(
            message.text,
            message.replyMessage?.messageId,
            message.image,
            message.audio,
            uid,
            onSuccess = { db.messageDao().updateAsSent(message.id) }
        ).also { db.messageDao().updateMessageId(message.id, it) }

    override suspend fun sendAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray
    ): Boolean {
        val fileName = message.getAttachment() ?: return false
        val fromGallery = message.image != null
        val byteArray = fileStore.getByteArray(fromGallery, fileName) ?: return false
        return firebase.uploadFile(fileName, uid, byteArray.convert(), isPrivate = true)
    }

    override suspend fun receiveAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray
    ): Boolean {
        val fileName = message.getAttachment() ?: return false
        val byteArray = firebase.downloadFile(fileName, uid, true) ?: return false
        val toGallery = message.image != null
        fileStore.saveByteArray(byteArray.convert(), fileName, toGallery)
        db.messageDao().updateAsReceived(message.id)
        return true
    }

    override fun notifyAsReceived(messageId: String) {
        firebase.sendReport(messageId, 1, 0)
    }

    override fun notifyAsNotReceived(messageId: String) {
        firebase.sendReport(messageId, -1, 0)
    }

    override fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean) {
        firebase.sendMessage(uid = uid, publicKey = publicKey, initiator = initiator)
    }

    override fun notifyAsViewed(message: Message) {
        firebase.sendReport(message.messageId, 1, 1)
        db.messageDao().updateAsViewed(message.id)
    }

    private fun getMyUID() = keyValueStore.getMyUID()
}