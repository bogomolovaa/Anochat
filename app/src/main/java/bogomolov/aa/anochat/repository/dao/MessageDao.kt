package bogomolov.aa.anochat.repository.dao

import androidx.paging.PagingSource
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
                "m.messageId as messageId, m.image as image, m.audio as audio,m.video as video, m.publicKey as publicKey, m.sent as sent, m.received as received, m.viewed as viewed, " +
                "r.id as reply_id, r.text as reply_text, r.time as reply_time, r.conversationId as reply_conversationId, r.isMine as reply_isMine, " +
                "r.messageId as reply_messageId, r.replyMessageId as reply_replyMessageId, r.image as reply_image, r.audio as reply_audio, r.video as reply_video, r.publicKey as reply_publicKey, r.sent as reply_sent, r.received as reply_received, r.viewed as reply_viewed, r.myUid as reply_myUid " +
                "FROM MessageEntity as m " +
                "LEFT JOIN MessageEntity as r ON (m.replyMessageId != '' and m.replyMessageId = r.messageId and m.conversationId = r.conversationId) where m.conversationId = :conversationId order by m.time desc"
    )
    fun loadAll(conversationId: Long): PagingSource<Int, MessageJoined>

    @Query("select m.image from MessageEntity as m LEFT JOIN ConversationEntity as c on m.conversationId = c.id where m.image is not null and c.userId = :userId")
    fun getImages(userId: Long): PagingSource<Int, String>

    @Query("select * from MessageEntity where messageId = :messageId")
    fun getByMessageId(messageId: String): MessageEntity?

    @Query("update MessageEntity set received = :received, viewed=:viewed, sent = 1 where messageId = :messageId and viewed <= :viewed")
    fun updateReport(messageId: String, received: Int, viewed: Int)

    @Transaction
    /*
    @Query(
        "select * from MessageEntity as message_ " +
                "LEFT JOIN ConversationEntity as conversation_ on message_.conversationId = conversation_.id " +
                "LEFT JOIN UserEntity as user_ on conversation_.userId = user_.id " +
                "where message_.text like :search and conversation_.myUid = :myUid"
    )
     */
    @Query(
        "SELECT c.id as conversation_id, c.userId as conversation_userId, c.lastMessageId as conversation_lastMessageId, c.myUid as conversation_myUid, " +
                "u.id as user_id, u.uid as user_uid, u.phone as user_phone, u.name as user_name, u.photo as user_photo, u.status as user_status, " +
                "m.id as message_id, m.text as message_text, m.time as message_time, m.conversationId as message_conversationId, m.isMine as message_isMine, m.messageId as message_messageId, " +
                "m.replyMessageId as message_replyMessageId, m.image as message_image, m.audio as message_audio, m.video as message_video, m.publicKey as message_publicKey, " +
                "m.sent as message_sent, m.received as message_received, m.viewed as message_viewed, m.myUid as message_myUid " +
                "FROM MessageEntity as m " +
                "LEFT JOIN ConversationEntity as c ON m.conversationId = c.id " +
                "LEFT JOIN UserEntity as u ON c.userId = u.id where m.text like :search and c.myUid = :myUid"
    )
    fun searchText(search: String, myUid: String): PagingSource<Int, ConversationJoined>

    @Query("select * from MessageEntity where myUid = :myUid and isMine = 1 and sent = 0 and conversationId in (select id from ConversationEntity where userId = :userId)")
    fun getNotSent(userId: Long, myUid: String): List<MessageEntity>

    @Query("update MessageEntity set viewed = 1 where id = :id")
    fun updateAsViewed(id: Long)

    @Query("update MessageEntity set received = 1 where id = :id")
    fun updateAsReceived(id: Long)

    @Query("update MessageEntity set received = -1 where id = :id")
    fun updateAsNotReceived(id: Long)

    @Query("update MessageEntity set messageId = :messageId, time = :time, sent = 1 where id = :id")
    fun updateAsSent(id: Long, messageId: String, time: Long)
}