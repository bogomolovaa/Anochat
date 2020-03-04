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

    override suspend fun getConversation(id: Long): Conversation =
        entityToModel(db.conversationDao().loadConversation(id))!!

    override suspend fun saveAndSendMessage(message: Message, conversation: Conversation) {
        saveMessage(message, conversation.id)
        firebase.sendMessage(message, conversation.user.uid)
    }

    override suspend fun saveMessage(message: Message, conversationId: Long) {
        message.id = db.messageDao().insert(modelToEntity(message))
        db.conversationDao().updateLastMessage(message.id, conversationId)
    }

    override suspend fun receiveMessage(text: String?, uid: String?, image: String?): Message? {
        if (uid != null) {
            val conversationEntity = getOrAddConversation(uid) { firebase.getUser(uid) }
            val message = Message(
                text = text ?: "",
                time = System.currentTimeMillis(),
                conversationId = conversationEntity.id,
                senderId = conversationEntity.userId,
                image = image
            )
            saveMessage(message, conversationEntity.id)
            return message
        }
        return null
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