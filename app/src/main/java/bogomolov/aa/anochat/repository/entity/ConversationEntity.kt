package bogomolov.aa.anochat.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ConversationEntity(){
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var userId: Long = 0
    var lastMessage: String = ""
    var lastTime: Long = 0

    constructor(id: Long, userId: Long, lastMessage: String, lastTime: Long): this() {
        this.id = id
        this.userId = userId
        this.lastMessage = lastMessage
        this.lastTime = lastTime
    }
}