package bogomolov.aa.anochat.repository.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val keyValueStore: KeyValueStore,
) : ConversationRepository {
    private val mapper = ModelEntityMapper()
    private var deleteConversationJob: Job? = null

    override suspend fun getConversation(id: Long): Conversation? =
        withContext(Dispatchers.IO) {
            mapper.entityToModel(db.conversationDao().loadConversation(id))
        }

    override fun loadConversationsDataSource() =
        Pager(PagingConfig(pageSize = 10)) {
            db.conversationDao().loadConversations(keyValueStore.getMyUID() ?: "")
        }.flow.map { it.map { mapper.entityToModel(it)!! } }.flowOn(Dispatchers.IO)

    override suspend fun deleteConversations(ids: Set<Long>) {
        withContext(Dispatchers.IO) {
            db.messageDao().deleteByConversationIds(ids)
            db.conversationDao().deleteByIds(ids)
        }
    }

    override fun deleteConversationIfNoMessages(conversationId: Long) {
        deleteConversationJob?.cancel()
        deleteConversationJob = CoroutineScope(Dispatchers.IO).launch {
            val number = db.messageDao().getMessagesNumber(conversationId)
            if (number == 0) db.conversationDao().deleteByIds(setOf(conversationId))
        }
    }

    override suspend fun createOrGetConversation(user: User): Long =
        withContext(Dispatchers.IO) {
            val myUid = keyValueStore.getMyUID()!!
            val conversationEntity = db.conversationDao().getConversationByUser(user.id, myUid)
                ?: ConversationEntity(userId = user.id, myUid = myUid).apply {
                    id = db.conversationDao().add(this)
                }
            conversationEntity.id
        }
}