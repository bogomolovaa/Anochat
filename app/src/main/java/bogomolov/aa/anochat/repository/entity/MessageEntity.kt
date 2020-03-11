package bogomolov.aa.anochat.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var text: String = "",
    var time: Long = 0,
    var conversationId: Long = 0,
    var senderId: Long = 0,
    @ColumnInfo(index = true)
    val messageId: String = "",
    @ColumnInfo(index = true)
    val replyMessageId: String? = null,
    var image: String? = null,
    var audio: String? = null,
    val received: Int = 0,
    val viewed: Int = 0
)

data class MessageJoined(
    var id: Long = 0,
    var text: String = "",
    var time: Long = 0,
    var conversationId: Long = 0,
    var senderId: Long = 0,
    val messageId: String = "",
    @Embedded(prefix = "reply_")
    val replyMessage: MessageEntity?,
    var image: String? = null,
    var audio: String? = null,
    val received: Int = 0,
    val viewed: Int = 0
)
