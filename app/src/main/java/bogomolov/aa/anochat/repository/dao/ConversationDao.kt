package bogomolov.aa.anochat.repository.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import bogomolov.aa.anochat.repository.entity.ConversationJoined

@Dao
interface ConversationDao {

    @Insert
    fun add(conversation: ConversationEntity): Long

    @Transaction
    @Query("SELECT ConversationEntity.*, UserEntity.*, MessageEntity.* FROM ConversationEntity " +
            "INNER JOIN UserEntity ON ConversationEntity.userId = UserEntity.id " +
            "INNER JOIN MessageEntity ON ConversationEntity.lastMessageId = MessageEntity.id"
    )
    fun loadConversations(): DataSource.Factory<Int, ConversationJoined>

}