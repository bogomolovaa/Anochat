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

    @Query("select * from MessageEntity where conversationId = :conversationId and senderId > 0 and viewed = 0")
    fun loadNotViewed(conversationId : Long): List<MessageEntity>

    @Query("update MessageEntity set received = 1, viewed = 1 where conversationId = :conversationId and senderId > 0 and viewed = 0")
    fun updateAsViewed(conversationId : Long)

    @Query("update MessageEntity set received = :received,  viewed=:viewed where messageId = :messageId")
    fun updateReport(messageId: String, received: Int, viewed: Int)

    @Query("update MessageEntity set messageId = :messageId where id = :id")
    fun updateMessageId(id: Long, messageId: String)


}