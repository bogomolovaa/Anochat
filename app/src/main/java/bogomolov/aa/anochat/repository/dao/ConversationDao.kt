package bogomolov.aa.anochat.repository.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import bogomolov.aa.anochat.repository.entity.ConversationEntity

@Dao
interface ConversationDao {

    @Insert
    fun add(conversation: ConversationEntity): Long

    @Query("select * from ConversationEntity")
    fun loadAll(): DataSource.Factory<Int, ConversationEntity>
}