package bogomolov.aa.anochat.repository.dao

import androidx.paging.DataSource
import androidx.room.*
import bogomolov.aa.anochat.repository.entity.ConversationJoined
import bogomolov.aa.anochat.repository.entity.MessageEntity
import bogomolov.aa.anochat.repository.entity.MessageJoined

@Dao
interface MessageDao {

    @Insert
    fun insert(message: MessageEntity): Long

    @Query("delete from MessageEntity where id in (:ids)")
    fun deleteByIds(ids: Set<Long>): Int

    @Query("delete from MessageEntity where conversationId in (:ids)")
    fun deleteByConversationIds(ids: Set<Long>)

    @Transaction
    @Query(
        "SELECT m.id as id, m.text as text, m.time as time, m.conversationId as conversationId, m.senderId as senderId, " +
                "m.messageId as messageId, m.replyMessageId as replyMessageId, m.image as image, m.audio as audio, m.publicKey as publicKey, m.sent as sent, m.received as received, m.viewed as viewed, " +
                "r.id as reply_id, r.text as reply_text, r.time as reply_time, r.conversationId as reply_conversationId, r.senderId as reply_senderId, " +
                "r.messageId as reply_messageId, r.replyMessageId as reply_replyMessageId, r.image as reply_image, r.audio as reply_audio, r.publicKey as reply_publicKey, r.sent as reply_sent, r.received as reply_received, r.viewed as reply_viewed, r.myUid as reply_myUid " +
                "FROM MessageEntity as m " +
                "LEFT JOIN MessageEntity as r ON (m.replyMessageId = r.messageId and m.conversationId = r.conversationId) where m.conversationId = :conversationId"
    )
    fun loadAll(conversationId: Long): DataSource.Factory<Int, MessageJoined>

    @Query("select m.image from MessageEntity as m LEFT JOIN ConversationEntity as c on m.conversationId = c.id where m.image is not null and c.userId = :userId")
    fun getImages(userId: Long): DataSource.Factory<Int, String>

    @Query("select * from MessageEntity where messageId = :messageId")
    fun getByMessageId(messageId: String): MessageEntity?

    @Query("update MessageEntity set received = 1, viewed = 1 where id = :id")
    fun updateAsViewed(id: Long)

    @Query("update MessageEntity set received = :received, viewed=:viewed where messageId = :messageId")
    fun updateReport(messageId: String, received: Int, viewed: Int)

    @Query("update MessageEntity set messageId = :messageId, sent = :sent where id = :id")
    fun updateMessageIdAndSent(id: Long, messageId: String, sent: Int)

    @Transaction
    @Query(
        "select * from MessageEntity as message_ " +
                "LEFT JOIN ConversationEntity as conversation_ on message_.conversationId = conversation_.id " +
                "LEFT JOIN UserEntity as user_ on conversation_.userId = user_.id " +
                "where message_.text like :search and conversation_.myUid = :myUid"
    )
    fun searchText(search: String, myUid: String): DataSource.Factory<Int, ConversationJoined>

    @Query("select * from MessageEntity where myUid = :myUid and sent = 0 and conversationId in (select id from ConversationEntity where userId = :userId)")
    fun getNotSent(userId: Long, myUid: String): List<MessageEntity>


}