package bogomolov.aa.anochat.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ConversationEntity{
    @PrimaryKey(autoGenerate = true)
    var id : Long = 0

}