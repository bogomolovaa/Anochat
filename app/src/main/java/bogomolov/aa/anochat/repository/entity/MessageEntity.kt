package bogomolov.aa.anochat.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class MessageEntity() {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var text : String = ""
    var time: Long = 0
    var conversationId: Long = 0
    var senderId: Long = 0

    constructor(id: Long, text: String, time: Long, conversationId: Long, senderId: Long) : this() {
        this.id = id
        this.text = text
        this.time = time
        this.conversationId = conversationId
        this.senderId = senderId
    }
}