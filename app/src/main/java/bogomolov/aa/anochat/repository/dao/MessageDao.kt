package bogomolov.aa.anochat.repository.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import bogomolov.aa.anochat.repository.entity.MessageEntity
import bogomolov.aa.anochat.repository.entity.MessageJoined

@Dao
interface MessageDao {

    @Insert
    fun insert(message: MessageEntity): Long

    @Transaction
    @Query(
        "SELECT m.id as id, m.text as text, m.time as time, m.conversationId as conversationId, m.senderId as senderId, " +
                "m.messageId as messageId, m.replyMessageId as replyMessageId, m.image as image,m.audio as audio, m.received as received, m.viewed as viewed, " +
                "r.id as reply_id, r.text as reply_text, r.time as reply_time, r.conversationId as reply_conversationId, r.senderId as reply_senderId, " +
                "r.messageId as reply_messageId, r.replyMessageId as reply_replyMessageId, r.image as reply_image, r.audio as reply_audio, r.received as reply_received, r.viewed as reply_viewed " +
                "FROM MessageEntity as m " +
                "LEFT JOIN MessageEntity as r ON (m.replyMessageId = r.messageId and m.conversationId = r.conversationId) where m.conversationId = :conversationId"
    )
    fun loadAll(conversationId: Long): DataSource.Factory<Int, MessageJoined>

    @Query("select * from MessageEntity where messageId = :messageId")
    fun getByMessageId(messageId: String): MessageEntity?

    @Query("select * from MessageEntity where conversationId = :conversationId and senderId > 0 and viewed = 0")
    fun loadNotViewed(conversationId: Long): List<MessageEntity>

    @Query("update MessageEntity set received = 1, viewed = 1 where conversationId = :conversationId and senderId > 0 and viewed = 0")
    fun updateAsViewed(conversationId: Long)

    @Query("update MessageEntity set received = :received,  viewed=:viewed where messageId = :messageId")
    fun updateReport(messageId: String, received: Int, viewed: Int)

    @Query("update MessageEntity set messageId = :messageId where id = :id")
    fun updateMessageId(id: Long, messageId: String)


}