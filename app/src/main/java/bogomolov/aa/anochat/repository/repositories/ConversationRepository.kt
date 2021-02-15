package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ConversationRepository"

@Singleton
class ConversationRepository @Inject constructor(
    private val db: AppDatabase,
    private val keyValueStore: KeyValueStore,
) {
    private val mapper = ModelEntityMapper()


    fun getConversation(id: Long): Conversation =
        mapper.entityToModel(db.conversationDao().loadConversation(id))!!

    fun loadConversationsDataSource() =
        db.conversationDao().loadConversations(keyValueStore.getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    fun deleteConversations(ids: Set<Long>) {
        db.messageDao().deleteByConversationIds(ids)
        db.conversationDao().deleteByIds(ids)
    }

    fun deleteConversationIfNoMessages(conversation: Conversation) {
        val number = db.messageDao().getMessagesNumber(conversation.id)
        if (number > 0) db.conversationDao().deleteByIds(setOf(conversation.id))
    }


    fun createOrGetConversation(user: User): Long {
        val myUid = keyValueStore.getMyUID()!!
        val conversationEntity = db.conversationDao().getConversationByUser(user.id, myUid)
            ?: ConversationEntity(userId = user.id, myUid = myUid).apply {
                id = db.conversationDao().add(this)
            }
        return conversationEntity.id
    }
}