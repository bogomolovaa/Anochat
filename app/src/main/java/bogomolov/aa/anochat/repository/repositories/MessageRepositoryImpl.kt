package bogomolov.aa.anochat.repository.repositories

import android.content.Context
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.getByteArray
import bogomolov.aa.anochat.features.shared.save
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MessageRepository"

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
    @ApplicationContext private val context: Context
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
        val byteArray = getByteArray(fromGallery, fileName, context) ?: return false
        return firebase.uploadFile(fileName, uid, byteArray.convert(), isPrivate = true)
    }

    override suspend fun receiveAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray
    ): Boolean {
        val fileName = message.getAttachment() ?: return false
        val byteArray = firebase.downloadFile(fileName, uid, true) ?: return false
        val toGallery = Settings.get(Settings.GALLERY, context) && message.image != null
        byteArray.convert().save(toGallery, fileName, context)
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