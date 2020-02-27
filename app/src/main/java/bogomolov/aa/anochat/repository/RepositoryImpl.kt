package bogomolov.aa.anochat.repository

import android.util.Log
import androidx.paging.DataSource
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import bogomolov.aa.anochat.repository.entity.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl
@Inject constructor(private val db: AppDatabase, private val firebase: FirebaseRepository) :
    Repository, IFirebaseRepository by firebase {

    override fun addMessage(message: Message) {
        message.id = db.messageDao().insert(modelToEntity(message))
    }

    override fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message> =
        db.messageDao().loadAll(conversationId).map {
            entityToModel(it)
        }

    override fun loadConversations(): DataSource.Factory<Int, Conversation> =
        db.conversationDao().loadAll().map {
            entityToModel(it)
        }

    override fun addConversation(conversation: Conversation) {
        conversation.id = db.conversationDao().add(modelToEntity(conversation))
    }


}