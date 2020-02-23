package bogomolov.aa.anochat.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class UserEntity {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var name: String = ""
}