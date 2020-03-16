package bogomolov.aa.anochat.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var uid: String = "",
    val phone: String,
    var name: String = "",
    var photo: String? = null,
    var status: String?
)