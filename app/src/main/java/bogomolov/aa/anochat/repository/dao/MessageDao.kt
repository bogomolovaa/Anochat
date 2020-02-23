package bogomolov.aa.anochat.repository.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import bogomolov.aa.anochat.repository.entity.MessageEntity

@Dao
interface MessageDao{

    @Insert
    fun insert(message: MessageEntity): Long

    @Query("select * from MessageEntity where conversationId = :conversationId")
    fun loadAll(conversationId : Long): DataSource.Factory<Int, MessageEntity>

    @Query("select * from MessageEntity where conversationId = :conversationId")
    fun loadAllMessages(conversationId : Long): List<MessageEntity>
}