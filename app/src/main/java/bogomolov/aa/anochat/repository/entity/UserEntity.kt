package bogomolov.aa.anochat.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class UserEntity() {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var uid: String = ""
    var name: String = ""
    var changed: Long = 0

    constructor(id: Long, uid: String, name: String, changed: Long): this() {
        this.id = id
        this.uid = uid
        this.name = name
        this.changed = changed
    }
}