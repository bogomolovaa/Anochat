package bogomolov.aa.anochat.repository.dao

import androidx.paging.DataSource
import androidx.room.*
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import bogomolov.aa.anochat.repository.entity.ConversationJoined

@Dao
interface ConversationDao {

    @Insert
    fun add(conversation: ConversationEntity): Long

    @Transaction
    @Query("SELECT * FROM ConversationEntity as conversation_ " +
            "LEFT JOIN UserEntity as user_ ON conversation_.userId = user_.id " +
            "LEFT JOIN MessageEntity as message_ ON conversation_.lastMessageId = message_.id order by message_.time desc"
    )
    fun loadConversations(): DataSource.Factory<Int, ConversationJoined>

    @Transaction
    @Query("SELECT * FROM ConversationEntity as conversation_ " +
            "LEFT JOIN UserEntity as user_ ON conversation_.userId = user_.id " +
            "LEFT JOIN MessageEntity as message_ ON conversation_.lastMessageId = message_.id where conversation_.id = :conversationId"
    )
    fun loadConversation(conversationId: Long): ConversationJoined

    @Query("SELECT * FROM ConversationEntity where userId = :userId LIMIT 1")
    fun getConversationByUser(userId: Long): ConversationEntity?

    @Query("UPDATE ConversationEntity set lastMessageId = :lastMessageId where id = :conversationId")
    fun updateLastMessage(lastMessageId: Long, conversationId: Long)
}