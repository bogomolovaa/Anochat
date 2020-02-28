package bogomolov.aa.anochat.repository.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var userId: Long = 0,
    var lastMessageId: Long = 0
)

data class ConversationJoined(
    @Embedded val conversation: ConversationEntity,
    @Embedded val user: UserEntity,
    @Embedded val lastMessage: MessageEntity
)
