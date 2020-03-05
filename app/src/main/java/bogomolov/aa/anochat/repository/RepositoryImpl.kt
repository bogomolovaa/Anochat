package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Log
import androidx.paging.DataSource
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import java.lang.Exception
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
        message.messageId = firebase.sendMessage(message.text, message.image, conversation.user.uid)
        saveMessage(message, conversation.id)
    }

    override suspend fun sendMessage(message: Message) {
        val conversation = getConversation(message.conversationId)
        message.messageId = firebase.sendMessage(message.text, message.image, conversation.user.uid)
        db.messageDao().updateMessageId(message.id, message.messageId)
    }

    override suspend fun saveMessage(message: Message, conversationId: Long) {
        message.id = db.messageDao().insert(modelToEntity(message))
        db.conversationDao().updateLastMessage(message.id, conversationId)
    }

    override fun receiveReport(messageId: String, received: Int, viewed: Int) {
        db.messageDao().updateReport(messageId, received, viewed)
    }

    override suspend fun receiveMessage(
        text: String?,
        uid: String,
        messageId: String,
        image: String?
    ): Message? {
        Log.i("test","getOrAddConversation")
        val conversationEntity = getOrAddConversation(uid) { firebase.getUser(uid) }
        val message = Message(
            text = text ?: "",
            time = System.currentTimeMillis(),
            conversationId = conversationEntity.id,
            senderId = conversationEntity.userId,
            messageId = messageId,
            image = image
        )
        Log.i("test","saveMessage")
        saveMessage(message, conversationEntity.id)
        firebase.sendReport(messageId, 1, 0)
        return message
    }

    private suspend fun getOrAddConversation(
        uid: String,
        getUser: suspend () -> User
    ): ConversationEntity {
        val userEntity = db.userDao().findByUid(uid)
        val userId = userEntity?.id ?: db.userDao().add(modelToEntity(getUser()))
        var conversationEntity = db.conversationDao().getConversationByUser(userId)
        if (conversationEntity == null) {
            conversationEntity = ConversationEntity(userId = userId)
            conversationEntity.id = db.conversationDao().add(conversationEntity)
        }
        return conversationEntity
    }


    override fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message> =
        db.messageDao().loadAll(conversationId).map {
            entityToModel(it)
        }

    override fun loadConversations(): DataSource.Factory<Int, Conversation> =
        db.conversationDao().loadConversations().map {
            entityToModel(it)
        }


    override suspend fun getConversation(user: User): Long =
        getOrAddConversation(user.uid) { user }.id


}