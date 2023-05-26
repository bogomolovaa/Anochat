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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    override suspend fun startTypingTo(uid: String) {
        withContext(Dispatchers.IO) {
            firebase.sendTyping(getMyUID()!!, uid, 1)
        }
    }

    override suspend fun stopTypingTo(uid: String) {
        withContext(Dispatchers.IO) {
            firebase.sendTyping(getMyUID()!!, uid, 0)
        }
    }

    override fun searchMessagesDataSource(search: String) =
        Pager(PagingConfig(pageSize = 10)) {
            db.messageDao().searchText("%$search%", getMyUID()!!)
        }.flow.map { it.map { mapper.entityToModel(it)!! } }.flowOn(Dispatchers.IO)


    override fun loadMessagesDataSource(conversationId: Long) =
        Pager(PagingConfig(pageSize = 30)) {
            db.messageDao().loadAll(conversationId)
        }.flow.map { it.map { mapper.entityToModel(it)!! } }.flowOn(Dispatchers.IO)

    override suspend fun deleteMessages(ids: Set<Long>) {
        withContext(Dispatchers.IO) {
            db.messageDao().deleteByIds(ids)
        }
    }

    override suspend fun receiveReport(messageId: String, received: Int, viewed: Int) {
        withContext(Dispatchers.IO) {
            db.messageDao().updateReport(messageId, received, viewed)
        }
    }

    override suspend fun saveMessage(message: Message): Long =
        withContext(Dispatchers.IO) {
            val entity = mapper.modelToEntity(message).apply { myUid = getMyUID()!! }
            db.messageDao().insert(entity).also { id ->
                db.conversationDao().updateLastMessage(id, message.conversationId)
            }
        }

    override suspend fun getPendingMessages(uid: String): List<Message> =
        withContext(Dispatchers.IO) {
            val userId = db.userDao().findByUid(uid)?.id ?: return@withContext listOf()
            mapper.entityToModel(db.messageDao().getNotSent(userId, getMyUID()!!))
        }

    override suspend fun getMessage(messageId: String) =
        withContext(Dispatchers.IO) {
            mapper.entityToModel(db.messageDao().getByMessageId(messageId))
        }

    override suspend fun sendMessage(message: Message, uid: String) =
        withContext(Dispatchers.IO) {
            firebase.sendMessage(message, uid) { db.messageDao().updateAsSent(message.id) }
                .also { db.messageDao().updateMessageId(message.id, it) }
        }

    override suspend fun sendAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = message.getAttachment() ?: return@withContext false
        val fromGallery = message.image != null
        val byteArray = fileStore.getByteArray(fromGallery, fileName) ?: return@withContext false
        firebase.uploadFile(fileName, uid, byteArray.convert(), isPrivate = true)
    }

    override suspend fun receiveAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray
    ) {
        withContext(Dispatchers.IO) {
            val fileName = message.getAttachment() ?: return@withContext
            val byteArray = firebase.downloadFile(fileName, uid, true) ?: run {
                db.messageDao().updateAsNotReceived(message.id)
                return@withContext
            }
            val toGallery = message.image != null || message.video != null
            fileStore.saveByteArray(byteArray.convert(), fileName, toGallery)
            message.video?.let { fileStore.createVideoThumbnail(it) }
            db.messageDao().updateAsReceived(message.id)
        }
    }

    override suspend fun notifyAsReceived(messageId: String) {
        withContext(Dispatchers.IO) {
            firebase.sendReport(messageId, 1, 0)
        }
    }

    override suspend fun notifyAsNotReceived(messageId: String) {
        withContext(Dispatchers.IO) {
            firebase.sendReport(messageId, -1, 0)
        }
    }

    override suspend fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean) {
        withContext(Dispatchers.IO) {
            firebase.sendMessage(uid = uid, publicKey = publicKey, initiator = initiator)
        }
    }

    override suspend fun notifyAsViewed(message: Message) {
        withContext(Dispatchers.IO) {
            firebase.sendReport(message.messageId, 1, 1)
            db.messageDao().updateAsViewed(message.id)
        }
    }

    private fun getMyUID() = keyValueStore.getMyUID()
}