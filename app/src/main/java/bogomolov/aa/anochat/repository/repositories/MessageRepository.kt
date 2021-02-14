package bogomolov.aa.anochat.repository.repositories

import android.content.Context
import android.util.Log
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MessageRepository"

@Singleton
class MessageRepository @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
    private val crypto: Crypto,
    context: Context
) {
    private val mapper = ModelEntityMapper()
    private val filesDir = getFilesDir(context)



    private val UID = "uid"
    private fun getMyUID() = keyValueStore.getValue<String>(UID)

    fun searchMessagesDataSource(search: String) =
        db.messageDao().searchText("%$search%", getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    fun loadMessagesDataSource(conversationId: Long, scope: CoroutineScope) =
        db.messageDao().loadAll(conversationId).map {
            mapper.entityToModel(it)!!
        }.mapByPage {
            scope.launch(Dispatchers.IO) {
                markAsViewed(it)
            }
            it
        }

    suspend fun receiveMessage(
        text: String,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?
    ) = if (canReceiveMessage(messageId, uid)) {
        Log.i(TAG, "receiveMessage $messageId $text replyId $replyId")
        val conversationEntity = getOrAddConversation(uid)
        val message = Message(
            time = System.currentTimeMillis(),
            conversationId = conversationEntity.id,
            senderId = conversationEntity.userId,
            messageId = messageId,
            image = image,
            audio = audio
        )
        val myUid = getMyUID()!!
        if (text.isNotEmpty()) message.text = crypto.decryptString(text, uid, myUid)
        if (!replyId.isNullOrEmpty()) message.replyMessage =
            mapper.entityToModel(db.messageDao().getByMessageId(replyId))
        if (message.image != null)
            downloadFile(message.image, uid, needDecrypt = true)
        if (message.audio != null)
            downloadFile(message.audio, uid, needDecrypt = true)
        saveMessage(message)
        firebase.sendReport(messageId, 1, 0)
        message
    } else {
        Log.i(TAG, "not received message $messageId from $uid: null secretKey")
        firebase.sendReport(messageId, -1, 0)
        sendPublicKey(uid, true)
        null
    }

    fun sendMessage(message: Message, uid: String) {
        Log.i("test", "sendMessage message")
        if (message.id == 0L) saveMessage(message)
        val myUid = getMyUID()!!
        val secretKey = crypto.getSecretKey(uid, myUid)
        if (secretKey != null) {
            val file = message.audio ?: message.image
            if (file != null) uploadFile(file, uid, needEncrypt = true)
            val text = crypto.encryptString(secretKey, message.text)
            message.messageId = firebase.sendMessage(
                text,
                message.replyMessage?.messageId,
                message.image,
                message.audio,
                uid
            )
            db.messageDao().updateMessageIdAndSent(message.id, message.messageId)
        } else {
            sendPublicKey(uid, true)
        }
    }

    fun deleteMessages(ids: Set<Long>) {
        db.messageDao().deleteByIds(ids)
    }

    fun sendPendingMessages(uid: String) {
        for (message in getPendingMessages(uid)) sendMessage(message, uid)
    }

    fun generateSecretKey(publicKey: String, uid: String): Boolean {
        val myUid = getMyUID()!!
        val generated = crypto.generateSecretKey(publicKey, uid, myUid)
        if (generated) {
            Log.i(TAG, "secret key generated, send messages")
            val sentSettingName = getSentSettingName(uid)
            keyValueStore.setValue(sentSettingName, false)
        } else {
            Log.i(TAG, "secret key not generated: privateKey null")
        }
        return generated
    }

    fun sendPublicKey(uid: String, initiator: Boolean) {
        val sentSettingName = getSentSettingName(uid)
        val isSent: Boolean = keyValueStore.getValue<Boolean>(sentSettingName) ?: false
        if (!isSent) {
            val myUid = getMyUID()!!
            val publicKey = crypto.generatePublicKey(uid, myUid)
            if (publicKey != null) {
                firebase.sendMessage(uid = uid, publicKey = publicKey, initiator = initiator)
                keyValueStore.setValue(sentSettingName, true)
                Log.i(TAG, "send publicKey for $uid")
            } else {
                Log.i(TAG, "publicKey not generated for $uid")
            }
        }
    }

    fun receiveReport(messageId: String, received: Int, viewed: Int) {
        Log.i(TAG, "receiveReport received $received viewed $viewed")
        db.messageDao().updateReport(messageId, received, viewed)
        if (viewed == 1 || received == -1) firebase.deleteRemoteMessage(messageId)
    }





    private fun uploadFile(fileName: String, uid: String, needEncrypt: Boolean = false) {
        val localFile = File(filesDir, fileName)
        val byteArray = if (needEncrypt) crypto.encryptFile(localFile, uid, getMyUID()!!)
        else localFile.readBytes()
        if (byteArray != null) {
            firebase.uploadFile(fileName, uid, byteArray, needEncrypt)
        } else {
            Log.w(TAG, "not uploaded: can't read file $fileName")
        }
    }

    private suspend fun downloadFile(fileName: String, uid: String, needDecrypt: Boolean = false) {
        val localFile = File(filesDir, fileName)
        firebase.downloadFile(fileName, uid, localFile, needDecrypt)
        if (needDecrypt) crypto.decryptFile(localFile, uid, getMyUID()!!)
    }

    private fun getSentSettingName(uid: String) = "${getMyUID()!!}${uid}_sent"

    private fun getPendingMessages(uid: String): List<Message> {
        val myUid = getMyUID()!!
        val userId = db.userDao().findByUid(uid)?.id
        return if (userId != null)
            mapper.entityToModel(db.messageDao().getNotSent(userId, myUid))
        else listOf()
    }

    private fun saveMessage(message: Message) {
        val entity = mapper.modelToEntity(message)
        entity.myUid = getMyUID()!!
        message.id = db.messageDao().insert(entity)
        Log.i(TAG, "save message ${message.id} $entity")
        db.conversationDao().updateLastMessage(message.id, message.conversationId)
    }

    private fun canReceiveMessage(messageId: String, uid: String) =
        crypto.getSecretKey(uid, getMyUID()!!) != null

    private fun markAsViewed(messages: List<Message>) {
        val ids = ArrayList<Long>()
        for (message in messages)
            if (!message.isMine() && message.viewed == 0) {
                message.viewed = 1
                firebase.sendReport(message.messageId, 1, 1)
                ids.add(message.id)
            }
        if (ids.size > 0) db.messageDao().updateAsViewed(ids)
    }
}