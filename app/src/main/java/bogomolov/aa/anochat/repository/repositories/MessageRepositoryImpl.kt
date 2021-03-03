package bogomolov.aa.anochat.repository.repositories

import android.content.Context
import android.util.Log
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
        db.messageDao().loadAll(conversationId).map {
            mapper.entityToModel(it)!!
        }

    override fun deleteMessages(ids: Set<Long>) {
        db.messageDao().deleteByIds(ids)
    }

    override fun receiveReport(messageId: String, received: Int, viewed: Int) {
        Log.d(TAG, "receiveReport received $received viewed $viewed")
        db.messageDao().updateReport(messageId, received, viewed)
        if (viewed == 1 || received == -1) firebase.deleteRemoteMessage(messageId)
    }

    override fun saveMessage(message: Message) {
        val entity = mapper.modelToEntity(message)
        entity.myUid = getMyUID()!!
        message.id = db.messageDao().insert(entity)
        Log.i(TAG, "save message ${message.id} $entity")
        db.conversationDao().updateLastMessage(message.id, message.conversationId)
    }

    override fun getPendingMessages(uid: String): List<Message> {
        val myUid = getMyUID()!!
        val userId = db.userDao().findByUid(uid)?.id
        return if (userId != null)
            mapper.entityToModel(db.messageDao().getNotSent(userId, myUid))
        else listOf()
    }

    override fun getMessage(messageId: String) =
        mapper.entityToModel(db.messageDao().getByMessageId(messageId))

    override fun sendMessage(message: Message, uid: String) {
        message.messageId = firebase.sendMessage(
            message.text,
            message.replyMessage?.messageId,
            message.image,
            message.audio,
            uid,
            onSuccess = {
                GlobalScope.launch(Dispatchers.IO) {
                    db.messageDao().updateAsSent(message.id)
                }
            }
        )
        db.messageDao().updateMessageId(message.id, message.messageId)
    }

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

    override fun notifyAsViewed(messages: List<Message>) {
        val ids = ArrayList<Long>()
        for (message in messages)
            if (!message.isMine && message.viewed == 0) {
                message.viewed = 1
                firebase.sendReport(message.messageId, 1, 1)
                ids.add(message.id)
            }
        if (ids.size > 0) db.messageDao().updateAsViewed(ids)
    }

    private fun getMyUID() = keyValueStore.getMyUID()
}