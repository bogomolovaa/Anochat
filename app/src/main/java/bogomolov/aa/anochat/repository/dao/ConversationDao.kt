package bogomolov.aa.anochat.repository.dao

import androidx.paging.PagingSource
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
    @Query(
        "SELECT c.id as conversation_id, c.userId as conversation_userId, c.lastMessageId as conversation_lastMessageId, c.myUid as conversation_myUid, " +
                "u.id as user_id, u.uid as user_uid, u.phone as user_phone, u.name as user_name, u.photo as user_photo, u.status as user_status, " +
                "m.id as message_id, m.text as message_text, m.time as message_time, m.conversationId as message_conversationId, m.isMine as message_isMine, m.messageId as message_messageId, " +
                "m.replyMessageId as message_replyMessageId, m.image as message_image, m.audio as message_audio, m.video as message_video, m.publicKey as message_publicKey, " +
                "m.sent as message_sent, m.received as message_received, m.viewed as message_viewed, m.myUid as message_myUid" +
                " FROM ConversationEntity as c " +
                "LEFT JOIN UserEntity as u ON c.userId = u.id " +
                "LEFT JOIN MessageEntity as m ON c.lastMessageId = m.id where c.myUid = :myUid order by m.time desc"
    )
    fun loadConversations(myUid: String): PagingSource<Int, ConversationJoined>

    @Transaction
    @Query(
        "SELECT c.id as conversation_id, c.userId as conversation_userId, c.lastMessageId as conversation_lastMessageId, c.myUid as conversation_myUid, " +
                "u.id as user_id, u.uid as user_uid, u.phone as user_phone, u.name as user_name, u.photo as user_photo, u.status as user_status, " +
                "m.id as message_id, m.text as message_text, m.time as message_time, m.conversationId as message_conversationId, m.isMine as message_isMine, m.messageId as message_messageId, " +
                "m.replyMessageId as message_replyMessageId, m.image as message_image, m.audio as message_audio, m.video as message_video, m.publicKey as message_publicKey, " +
                "m.sent as message_sent, m.received as message_received, m.viewed as message_viewed, m.myUid as message_myUid" +
                " FROM ConversationEntity as c " +
                "LEFT JOIN UserEntity as u ON c.userId = u.id " +
                "LEFT JOIN MessageEntity as m ON c.lastMessageId = m.id where c.id = :conversationId"
    )
    fun loadConversation(conversationId: Long): ConversationJoined

    @Query("SELECT * FROM ConversationEntity where userId = :userId and myUid = :myUid")
    fun getConversationByUser(userId: Long, myUid: String): ConversationEntity?

    @Query("UPDATE ConversationEntity set lastMessageId = :lastMessageId where id = :conversationId")
    fun updateLastMessage(lastMessageId: Long, conversationId: Long)

    @Query("delete from ConversationEntity where id in (:ids)")
    fun deleteByIds(ids: Set<Long>)

}