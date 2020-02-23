package bogomolov.aa.anochat.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import bogomolov.aa.anochat.repository.entity.ConversationEntity

@Dao
interface ConversationDao {

    @Insert
    fun insert(conversation: ConversationEntity): Long

    @Query("select * from ConversationEntity")
    fun loadAll(): List<ConversationEntity>
}