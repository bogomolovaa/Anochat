package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Log
import androidx.paging.DataSource
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.conversations.*
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Repository"

@Singleton
class RepositoryImpl
@Inject constructor(
    private val db: AppDatabase,
    private val firebase: FirebaseRepository,
    private val context: Context
) : Repository, IFirebaseRepository by firebase {

    init {
        firebase.setRepository(this)
    }

    private val mapper = ModelEntityMapper(this)
    private val crypto = Crypto(this)

    override fun getCrypto() = crypto

    override fun getContext() = context

    override suspend fun getConversation(id: Long): Conversation =
        mapper.entityToModel(db.conversationDao().loadConversation(id))!!

    override suspend fun sendPublicKey(uid: String, initiator: Boolean) {
        val sentSettingName = getSentSettingName(uid)
        val isSent: Boolean = getSetting<Boolean>(sentSettingName) ?: false
        if (!isSent) {
            val publicKey = crypto.generatePublicKey(uid)
            if (publicKey != null) {
                firebase.sendMessage(uid = uid, publicKey = publicKey, initiator = initiator)
                setSetting(sentSettingName, true)
                Log.i(TAG, "send publicKey for $uid")
            } else {
                Log.i(TAG, "publicKey not generated for $uid")
            }
        }
    }

    override suspend fun sendMessage(message: Message, uid: String) {
        Log.i("test", "sendMessage message")
        val secretKey = crypto.getSecretKey(uid)
        if (secretKey != null) {
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

    override suspend fun sendPendingMessages(uid: String) {
        for (message in getPendingMessages(uid)) sendMessage(message, uid)
    }

    private fun getPendingMessages(uid: String): List<Message> {
        val myUid = getMyUID()!!
        val userId = db.userDao().findByUid(uid)?.id
        return if (userId != null)
            mapper.entityToModel(db.messageDao().getNotSent(userId, myUid))
        else listOf()
    }

    override suspend fun saveMessage(message: Message) {
        val entity = mapper.modelToEntity(message)
        message.id = db.messageDao().insert(entity)
        Log.i(TAG, "save message $entity")
        db.conversationDao().updateLastMessage(message.id, message.conversationId)
    }

    override suspend fun receiveReport(messageId: String, received: Int, viewed: Int) {
        Log.i(TAG, "receiveReport received $received viewed $viewed")
        db.messageDao().updateReport(messageId, received, viewed)
        if (received != 0) firebase.deleteRemoteMessage(messageId)
    }

    override suspend fun receiveMessage(
        text: String,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?
    ): Message {
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
        if (message.text.isNotEmpty()) message.text = crypto.decryptString(text, uid)
        if (!replyId.isNullOrEmpty()) message.replyMessage =
            mapper.entityToModel(db.messageDao().getByMessageId(replyId))
        saveMessage(message)
        firebase.sendReport(messageId, 1, 0)
        if (message.image != null)
            firebase.downloadFile(message.image, uid, true)
        if (message.audio != null)
            firebase.downloadFile(message.audio, uid, true)
        return message
    }

    override fun getImagesDataSource(userId: Long) = db.messageDao().getImages(userId)

    override suspend fun getMyUser(): User {
        val myUid = getMyUID()!!
        var user = mapper.entityToModel(db.userDao().findByUid(myUid))
        if (user == null) {
            user = receiveUser(myUid)
            syncFromRemoteUser(user!!, saveLocal = true, loadFullPhoto = true)
        }
        return user
    }

    override suspend fun updateMyUser(user: User) {
        val savedUser = db.userDao().getUser(user.id)
        if (user.name != savedUser.name) firebase.renameUser(user.uid, user.name)
        if (user.status != savedUser.status) firebase.updateStatus(user.uid, user.status)
        if (user.photo != null && user.photo != savedUser.photo)
            firebase.updatePhoto(user.uid, user.photo)
        db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
    }

    override suspend fun getUser(id: Long): User = mapper.entityToModel(db.userDao().getUser(id))!!

    override suspend fun receiveUser(uid: String): User? = firebase.getUser(uid)

    override suspend fun syncFromRemoteUser(
        user: User,
        saveLocal: Boolean,
        loadFullPhoto: Boolean
    ) {
        val savedUser = db.userDao().findByUid(user.uid)
        if (savedUser != null) {
            db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
            if ((user.photo != savedUser.photo && user.photo != null)) {
                if (loadFullPhoto) downloadPhoto(user.photo, user.uid)
                downloadMiniPhoto(user.photo, user.uid)
            }
        } else {
            if (saveLocal) user.id = db.userDao().add(mapper.modelToEntity(user))
            if (user.photo != null) {
                if (loadFullPhoto) downloadPhoto(user.photo, user.uid)
                downloadMiniPhoto(user.photo, user.uid)
            }
        }
    }

    private suspend fun downloadPhoto(photo: String, uid: String) {
        if (!File(getFilesDir(context), photo).exists())
            firebase.downloadFile(photo, uid)
    }

    private suspend fun downloadMiniPhoto(photo: String, uid: String) {
        val miniPhoto = getMiniPhotoFileName(context, photo)
        if (!File(getFilesDir(context), miniPhoto).exists())
            firebase.downloadFile(miniPhoto, uid)
    }

    private suspend fun getOrAddConversation(uid: String): ConversationEntity {
        val myUid = getMyUID()!!
        val userEntity = db.userDao().findByUid(uid)
        val userId = if (userEntity == null) {
            val user = receiveUser(uid)
            if (user != null) syncFromRemoteUser(user, saveLocal = true, loadFullPhoto = true)
            user?.id ?: 0L
        } else {
            userEntity.id
        }
        var conversationEntity = db.conversationDao().getConversationByUser(userId, myUid)
        if (conversationEntity == null) {
            conversationEntity = ConversationEntity(userId = userId, myUid = myUid)
            conversationEntity.id = db.conversationDao().add(conversationEntity)
        }
        return conversationEntity
    }

    override fun loadMessagesDataSource(conversationId: Long): DataSource.Factory<Int, Message> =
        db.messageDao().loadAll(conversationId).map {
            mapper.entityToModel(it)
        }

    override suspend fun loadAllConversations(): List<Conversation> {
        val myUid = getMyUID()
        return if (myUid == null) listOf()
        else mapper.entityToModel(db.conversationDao().loadAllConversations(myUid))
    }

    override fun loadConversationsDataSource(): DataSource.Factory<Int, Conversation> {
        val myUid = getMyUID() ?: ""
        return db.conversationDao().loadConversations(myUid).map {
            mapper.entityToModel(it)
        }
    }

    override suspend fun getConversation(user: User) = getOrAddConversation(user.uid).id

    override fun searchMessagesDataSource(search: String): DataSource.Factory<Int, Conversation> {
        val myUid = getMyUID()!!
        return db.messageDao().searchText("%$search%", myUid).map {
            mapper.entityToModel(it)
        }
    }

    override fun getUsersByPhonesDataSource(phones: List<String>): DataSource.Factory<Int, User> {
        val myUid = getMyUID()!!
        return db.userDao().getAll(phones, myUid).map {
            mapper.entityToModel(it)
        }
    }

    override suspend fun deleteConversationIfNoMessages(conversationId: Long) {
        val messages = db.messageDao().getMessages(conversationId)
        if (messages.isEmpty()) db.conversationDao().deleteByIds(setOf(conversationId))
    }

    override suspend fun deleteMessages(ids: Set<Long>) {
        db.messageDao().deleteByIds(ids)
    }

    override suspend fun deleteConversations(ids: Set<Long>) {
        db.messageDao().deleteByConversationIds(ids)
        db.conversationDao().deleteByIds(ids)
    }
}