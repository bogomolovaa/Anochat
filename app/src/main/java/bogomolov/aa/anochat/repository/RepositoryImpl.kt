package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Log
import androidx.paging.DataSource
import bogomolov.aa.anochat.android.*
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl
@Inject constructor(
    private val db: AppDatabase,
    private val firebase: FirebaseRepository,
    private val context: Context
) :
    Repository, IFirebaseRepository by firebase {
    companion object {
        private const val TAG = "Repository"
    }

    private val mapper = ModelEntityMapper(context)

    override fun getContext() = context

    override suspend fun reportAsViewed(conversationId: Long) {
        val messages = db.messageDao().loadNotViewed(conversationId)
        for (message in messages) firebase.sendReport(message.messageId, 1, 1)
        db.messageDao().updateAsViewed(conversationId)
    }

    override suspend fun getConversation(id: Long): Conversation =
        mapper.entityToModel(db.conversationDao().loadConversation(id))!!

    override suspend fun sendPublicKey(uid: String, initiator: Boolean) {
        val myUid = getMyUid(context)!!
        val sentSettingName = getSentSettingName(myUid,uid)
        val isSent = getSetting<Boolean>(context, sentSettingName)!!
        if (initiator && !isSent) {
            Log.i("test", "sendKey $uid")
            setSetting(context,sentSettingName,true)
            val keyPair = createKeyPair()
            val publicKeyByteArray = keyPair?.public?.encoded
            val privateKey = keyPair?.private
            if (publicKeyByteArray != null && privateKey != null) {
                saveKey(getPrivateKeyName(myUid, uid), privateKey, context)
                val publicKey = byteArrayToBase64(publicKeyByteArray)
                firebase.sendMessage(
                    null,
                    null,
                    null,
                    null,
                    uid,
                    publicKey,
                    initiator
                )
            } else {
                Log.i(TAG, "null keyPair")
            }
        }
    }

    override suspend fun getPendingMessages(uid: String): List<Message> {
        val myUid = getMyUid(context)!!
        val userId = db.userDao().findByUid(uid)?.id
        return if (userId != null)
            mapper.entityToModel(db.messageDao().getNotSent(userId, myUid))
        else listOf()
    }

    override suspend fun sendMessage(message: Message) {
        Log.i("test", "sendMessage message")
        val conversation = getConversation(message.conversationId)
        val myUid = getMyUid(context)!!
        val secretKey = getSecretKey(myUid, conversation.user.uid, context)
        if (secretKey != null) {
            val text = encryptString(secretKey, message.text)
            message.messageId = firebase.sendMessage(
                text,
                message.replyMessage?.messageId,
                message.image,
                message.audio,
                conversation.user.uid,
                null
            )
            db.messageDao().updateMessageIdAndSent(message.id, message.messageId, 1)
        } else {
            sendPublicKey(conversation.user.uid, true)
        }
    }

    override suspend fun saveMessage(message: Message, conversationId: Long) {
        val entity = mapper.modelToEntity(message)
        message.id = db.messageDao().insert(entity)
        Log.i("test", "save message $entity")
        db.conversationDao().updateLastMessage(message.id, conversationId)
    }

    override fun receiveReport(messageId: String, received: Int, viewed: Int) {
        db.messageDao().updateReport(messageId, received, viewed)
    }

    override suspend fun receiveMessage(
        text: String?,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?
    ): Message? {
        val conversationEntity = getOrAddConversation(uid) { firebase.getUser(uid)!! }
        val secretKey = getSecretKey(getMyUid(context)!!, uid, context)!!
        val message = Message(
            text = text ?: "",
            time = System.currentTimeMillis(),
            conversationId = conversationEntity.id,
            senderId = conversationEntity.userId,
            messageId = messageId,
            replyMessage = if (replyId != null)
                mapper.entityToModel(db.messageDao().getByMessageId(replyId)) else null,
            image = image,
            audio = audio
        )
        if (message.text.isNotEmpty()) message.text = decryptString(secretKey, message.text)
        saveMessage(message, conversationEntity.id)
        firebase.sendReport(messageId, 1, 0)
        if (message.image != null)
            firebase.downloadFile(message.image, uid, true)
        if (message.audio != null)
            firebase.downloadFile(message.audio, uid, true)
        return message
    }

    override fun getImages(userId: Long) = db.messageDao().getImages(userId)

    override suspend fun getUser(uid: String): User? {
        Log.i("test", "getUser $uid")
        var user = mapper.entityToModel(db.userDao().findByUid(uid))
        if (user == null) {
            user = firebase.getUser(uid)
            updateUserFrom(user = user!!, saveLocal = true)
            Log.i("test", "user ${user.uid} updated")
        }
        return user
    }

    override suspend fun receiveUser(uid: String): User? = firebase.getUser(uid)

    override suspend fun getUser(id: Long): User = mapper.entityToModel(db.userDao().getUser(id))!!

    override suspend fun updateUserTo(user: User) {
        val savedUser = db.userDao().getUser(user.id)
        Log.i("test", "updateUserTo $user saved: $savedUser")
        if (savedUser != null) {
            if (user.name != savedUser.name) firebase.renameUser(user.uid, user.name)
            if (user.status != savedUser.status) firebase.updateStatus(user.uid, user.status)
            if (user.photo != null && user.photo != savedUser.photo)
                firebase.updatePhoto(user.uid, user.photo!!)
            db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
        }
    }

    override suspend fun loadConversations(): List<Conversation> {
        val myUid = getSetting<String>(context, UID)!!
        return mapper.entityToModel(db.conversationDao().loadAllConversations(myUid))
    }

    private suspend fun loadPhoto(user: User, loadFullPhoto: Boolean) {
        val photo = user.photo!!
        val miniPhoto = getMiniPhotoFileName(context, photo)
        if (loadFullPhoto && !File(getFilesDir(context), photo).exists())
            firebase.downloadFile(photo, user.uid)
        if (!File(getFilesDir(context), miniPhoto).exists())
            firebase.downloadFile(miniPhoto, user.uid)
    }

    override suspend fun updateUserFrom(user: User, saveLocal: Boolean) {
        val savedUser = db.userDao().getUser(user.id)
        if (savedUser != null) {
            if ((user.photo != savedUser.photo && user.photo != null)) loadPhoto(user, true)
            db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
        } else {
            if (saveLocal) user.id = db.userDao().add(mapper.modelToEntity(user))
            if (user.photo != null) loadPhoto(user, saveLocal)
        }
    }

    private suspend fun getOrAddConversation(
        uid: String,
        getUser: suspend () -> User
    ): ConversationEntity {
        val myUid = getSetting<String>(context, UID)!!
        val userEntity = db.userDao().findByUid(uid)
        val userId = if (userEntity == null) {
            val user = getUser()
            updateUserFrom(user, true)
            user.id
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


    override fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message> =
        db.messageDao().loadAll(conversationId).map {
            mapper.entityToModel(it)
        }

    override fun loadConversationsDataSource(): DataSource.Factory<Int, Conversation> {
        val myUid = getSetting<String>(context, UID) ?: ""
        return db.conversationDao().loadConversations(myUid).map {
            mapper.entityToModel(it)
        }
    }

    override suspend fun getConversation(user: User): Long =
        getOrAddConversation(user.uid) { user }.id


    override fun searchMessagesDataSource(search: String): DataSource.Factory<Int, Conversation> {
        val myUid = getSetting<String>(context, UID)!!
        return db.messageDao().searchText("%$search%", myUid).map {
            mapper.entityToModel(it)
        }
    }
}