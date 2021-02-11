package bogomolov.aa.anochat.repository.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var userId: Long = 0,
    var lastMessageId: Long = 0,
    var myUid: String
)

data class ConversationJoined(
    @Embedded(prefix = "conversation_")
    val conversation: ConversationEntity,
    @Embedded(prefix = "user_")
    val user: UserEntity,
    @Embedded(prefix = "message_")
    val lastMessage: MessageEntity?
)
