package bogomolov.aa.anochat.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var text: String = "",
    var time: Long = 0,
    var conversationId: Long = 0,
    var senderId: Long = 0
)
