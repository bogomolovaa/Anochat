package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Log
import androidx.paging.DataSource
import bogomolov.aa.anochat.android.UID
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.android.getSetting
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    override fun getContext() = context

    override suspend fun reportAsViewed(conversationId: Long) {
        val messages = db.messageDao().loadNotViewed(conversationId)
        for (message in messages) firebase.sendReport(message.messageId, 1, 1)
        db.messageDao().updateAsViewed(conversationId)
    }

    override suspend fun getConversation(id: Long): Conversation =
        entityToModel(db.conversationDao().loadConversation(id))!!

    override suspend fun saveAndSendMessage(message: Message, conversation: Conversation) {
        message.messageId = firebase.sendMessage(
            message.text,
            message.replyMessage?.messageId,
            message.image,
            message.audio,
            conversation.user.uid
        )
        saveMessage(message, conversation.id)
    }

    override suspend fun sendMessage(message: Message) {
        val conversation = getConversation(message.conversationId)
        message.messageId = firebase.sendMessage(
            message.text,
            message.replyMessage?.messageId,
            message.image,
            message.audio,
            conversation.user.uid
        )
        db.messageDao().updateMessageId(message.id, message.messageId)
    }

    override suspend fun saveMessage(message: Message, conversationId: Long) {
        val entity = modelToEntity(message)
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
        val message = Message(
            text = text ?: "",
            time = System.currentTimeMillis(),
            conversationId = conversationEntity.id,
            senderId = conversationEntity.userId,
            messageId = messageId,
            replyMessage = if (replyId != null) entityToModel(db.messageDao().getByMessageId(replyId)) else null,
            image = image,
            audio = audio
        )
        saveMessage(message, conversationEntity.id)
        firebase.sendReport(messageId, 1, 0)
        return message
    }

    override fun getImages(userId: Long) = db.messageDao().getImages(userId)

    override suspend fun getUser(uid: String): User? {
        Log.i("test","getUser $uid")
        var user = entityToModel(db.userDao().findByUid(uid))
        if (user == null) {
            user = firebase.getUser(uid)
            updateUserFrom(user = user!!, saveLocal =  true)
            Log.i("test","user ${user.uid} updated")
        }
        return user
    }

    override suspend fun receiveUser(uid: String): User? = firebase.getUser(uid)

    override suspend fun getUser(id: Long): User = entityToModel(db.userDao().getUser(id))!!

    override suspend fun updateUserTo(user: User) {
        val savedUser = db.userDao().getUser(user.id)
        Log.i("test","updateUserTo $user saved: $savedUser")
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
        return entityToModel(db.conversationDao().loadAllConversations(myUid))
    }

    override suspend fun updateUserFrom(user: User, saveLocal: Boolean) {
        val savedUser = db.userDao().getUser(user.id)
        if (savedUser != null) {
            if ((user.photo != savedUser.photo && user.photo != null))
                firebase.downloadFile(user.photo!!, user.uid)
            db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
        } else {
            if (saveLocal) user.id = db.userDao().add(modelToEntity(user))
            if (user.photo != null && !File(getFilesDir(context), user.photo!!).exists())
                firebase.downloadFile(user.photo!!, user.uid)
        }
    }

    private suspend fun getOrAddConversation(
        uid: String,
        getUser: suspend () -> User
    ): ConversationEntity {
        val myUid = getSetting<String>(context, UID)!!
        val userEntity = db.userDao().findByUid(uid)
        val userId = userEntity?.id ?: db.userDao().add(modelToEntity(getUser()))
        var conversationEntity = db.conversationDao().getConversationByUser(userId, myUid)
        if (conversationEntity == null) {
            conversationEntity = ConversationEntity(userId = userId, myUid = myUid)
            conversationEntity.id = db.conversationDao().add(conversationEntity)
        }
        return conversationEntity
    }


    override fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message> =
        db.messageDao().loadAll(conversationId).map {
            entityToModel(it)
        }

    override fun loadConversationsDataSource(): DataSource.Factory<Int, Conversation> {
        val myUid = getSetting<String>(context, UID)!!
        return db.conversationDao().loadConversations(myUid).map {
            entityToModel(it)
        }
    }

    override suspend fun getConversation(user: User): Long =
        getOrAddConversation(user.uid) { user }.id


}