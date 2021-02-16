package bogomolov.aa.anochat.repository.repositories

import android.util.Log
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.getValue
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.domain.setValue
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.FILES_DIRECTORY
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MessageRepository"

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore
) : MessageRepository {
    private val filesDir: String = keyValueStore.getValue(FILES_DIRECTORY)!!
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
        Log.i(TAG, "receiveReport received $received viewed $viewed")
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
            uid
        )
        db.messageDao().updateMessageIdAndSent(message.id, message.messageId)
    }

    override fun getAttachmentFile(fileName: String) = File(filesDir, fileName)

    override suspend fun sendAttachment(fileName: String, uid: String, byteArray: ByteArray) {
        firebase.uploadFile(fileName, uid, byteArray, isPrivate = true)
    }

    override suspend fun receiveAttachment(fileName: String, uid: String, localFile: File) {
        firebase.downloadFile(fileName, uid, localFile, isPrivate = true)
    }

    override fun notifyAsReceived(messageId: String) {
        firebase.sendReport(messageId, 1, 0)
    }

    override fun notifyAsNotReceived(messageId: String) {
        firebase.sendReport(messageId, -1, 0)
    }

    override fun keyIsNotSentTo(uid: String): Boolean {
        val sentSettingName = getSentSettingName(uid)
        return !(keyValueStore.getValue<Boolean>(sentSettingName) ?: false)
    }

    override fun setKeyAsSentTo(uid: String) {
        val sentSettingName = getSentSettingName(uid)
        keyValueStore.setValue(sentSettingName, false)
    }

    override fun sendPublicKey(publicKey: String, uid: String, initiator: Boolean) {
        val sentSettingName = getSentSettingName(uid)
        firebase.sendMessage(uid = uid, publicKey = publicKey, initiator = initiator)
        keyValueStore.setValue(sentSettingName, true)
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

    private fun getSentSettingName(uid: String) = "${getMyUID()!!}${uid}_sent"
}