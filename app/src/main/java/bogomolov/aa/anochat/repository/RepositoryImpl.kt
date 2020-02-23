package bogomolov.aa.anochat.repository

import android.util.Log
import androidx.paging.DataSource
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import bogomolov.aa.anochat.repository.entity.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl
@Inject constructor(private val db: AppDatabase) : Repository {

    override fun addMessage(message: Message) {
        message.id = db.messageDao().insert(modelToEntity(message))
    }

    override fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message> {
        val factory = db.messageDao().loadAll(conversationId)
        return factory.map {
            entityToModel(it)
        }
    }

}