package bogomolov.aa.anochat.repository.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import kotlinx.coroutines.flow.map
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

    override fun startTypingTo(uid: String) {
        firebase.sendTyping(getMyUID()!!, uid, 1)
    }

    override fun stopTypingTo(uid: String) {
        firebase.sendTyping(getMyUID()!!, uid, 0)
    }

    override fun searchMessagesDataSource(search: String) =
        Pager(PagingConfig(pageSize = 10)) {
            db.messageDao().searchText("%$search%", getMyUID()!!)
        }.flow.map { it.map { mapper.entityToModel(it)!! } }


    override fun loadMessagesDataSource(conversationId: Long) =
        Pager(PagingConfig(pageSize = 10)) {
            db.messageDao().loadAll(conversationId)
        }.flow.map { it.map { mapper.entityToModel(it)!! } }

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
        firebase.sendMessage(message, uid) { db.messageDao().updateAsSent(message.id) }
            .also { db.messageDao().updateMessageId(message.id, it) }

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
    ) {
        val fileName = message.getAttachment() ?: return
        val byteArray = firebase.downloadFile(fileName, uid, true) ?: run {
            db.messageDao().updateAsNotReceived(message.id)
            return
        }
        val toGallery = message.image != null || message.video != null
        fileStore.saveByteArray(byteArray.convert(), fileName, toGallery)
        message.video?.let { fileStore.createVideoThumbnail(it) }
        db.messageDao().updateAsReceived(message.id)
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