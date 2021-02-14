package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.repository.*
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ConversationRepository"

@Singleton
class ConversationRepository @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
) {
    private val mapper = ModelEntityMapper()

    private val UID = "uid"
    private fun getMyUID() = keyValueStore.getValue<String>(UID)

    fun getConversation(id: Long): Conversation =
        mapper.entityToModel(db.conversationDao().loadConversation(id))!!

    fun loadConversationsDataSource() =
        db.conversationDao().loadConversations(getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    suspend fun createConversation(user: User) = getOrAddConversation(user.uid).id

    fun deleteConversations(ids: Set<Long>) {
        db.messageDao().deleteByConversationIds(ids)
        db.conversationDao().deleteByIds(ids)
    }

    fun deleteConversationIfNoMessages(conversation: Conversation) {
        val messages = db.messageDao().getMessages(conversation.id)
        if (messages.isEmpty()) db.conversationDao().deleteByIds(setOf(conversation.id))
    }


    private fun loadAllConversations(): List<Conversation> {
        val myUid = getMyUID()
        return if (myUid == null) listOf()
        else mapper.entityToModel(db.conversationDao().loadAllConversations(myUid))
    }

    private suspend fun getOrAddConversation(uid: String): ConversationEntity {
        val myUid = getMyUID()!!
        val userId = getOrAddUser(uid).id
        val conversationEntity = db.conversationDao().getConversationByUser(userId, myUid)
        return conversationEntity ?: ConversationEntity(userId = userId, myUid = myUid).apply {
            id = db.conversationDao().add(this)
        }
    }

    private suspend fun getOrAddUser(uid: String): User {
        val userEntity = db.userDao().findByUid(uid)
        return mapper.entityToModel(userEntity) ?: firebase.getUser(uid)!!
            .apply { syncFromRemoteUser(this) }
    }
}