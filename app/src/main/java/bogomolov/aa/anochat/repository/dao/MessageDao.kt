package bogomolov.aa.anochat.repository.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import bogomolov.aa.anochat.repository.entity.ConversationJoined
import bogomolov.aa.anochat.repository.entity.MessageEntity
import bogomolov.aa.anochat.repository.entity.MessageJoined

@Dao
interface MessageDao {

    @Insert
    fun insert(message: MessageEntity): Long

    @Query("select count(id) from MessageEntity where conversationId = :conversationId")
    fun getMessagesNumber(conversationId: Long): Int

    @Query("delete from MessageEntity where id in (:ids)")
    fun deleteByIds(ids: Set<Long>): Int

    @Query("delete from MessageEntity where conversationId in (:ids)")
    fun deleteByConversationIds(ids: Set<Long>)

    @Transaction
    @Query(
        "SELECT m.id as id, m.text as text, m.time as time, m.conversationId as conversationId, m.isMine as isMine, " +
                "m.messageId as messageId, m.image as image, m.audio as audio, m.publicKey as publicKey, m.sent as sent, m.received as received, m.viewed as viewed, " +
                "r.id as reply_id, r.text as reply_text, r.time as reply_time, r.conversationId as reply_conversationId, r.isMine as reply_isMine, " +
                "r.messageId as reply_messageId, r.replyMessageId as reply_replyMessageId, r.image as reply_image, r.audio as reply_audio, r.publicKey as reply_publicKey, r.sent as reply_sent, r.received as reply_received, r.viewed as reply_viewed, r.myUid as reply_myUid " +
                "FROM MessageEntity as m " +
                "LEFT JOIN MessageEntity as r ON (m.replyMessageId = r.messageId and m.conversationId = r.conversationId) where m.conversationId = :conversationId order by m.time desc"
    )
    fun loadAll(conversationId: Long): DataSource.Factory<Int, MessageJoined>

    @Query("select m.image from MessageEntity as m LEFT JOIN ConversationEntity as c on m.conversationId = c.id where m.image is not null and c.userId = :userId")
    fun getImages(userId: Long): DataSource.Factory<Int, String>

    @Query("select * from MessageEntity where messageId = :messageId")
    fun getByMessageId(messageId: String): MessageEntity?

    @Query("update MessageEntity set received = :received, viewed=:viewed, sent = 1 where messageId = :messageId and viewed <= :viewed")
    fun updateReport(messageId: String, received: Int, viewed: Int)

    @Query("update MessageEntity set messageId = :messageId where id = :id")
    fun updateMessageId(id: Long, messageId: String)

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

    @Query("update MessageEntity set viewed = 1 where id = :id")
    fun updateAsViewed(id: Long)

    @Query("update MessageEntity set received = 1 where id = :id")
    fun updateAsReceived(id: Long)

    @Query("update MessageEntity set sent = 1 where id = :id")
    fun updateAsSent(id: Long)

}