package bogomolov.aa.anochat.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import bogomolov.aa.anochat.repository.entity.UserEntity

@Dao
interface UserDao {

    @Insert
    fun add(user: UserEntity): Long

    @Query("select * from UserEntity where id = :id")
    fun getUser(id: Long): UserEntity

    @Query("select * from UserEntity where uid = :uid")
    fun findByUid(uid: String): UserEntity?

    @Update
    fun update(user: UserEntity)

    @Query("select * from UserEntity where phone in (:phoneList) and uid != :myUid")
    fun getAll(phoneList: List<String>, myUid: String): List<UserEntity>

    @Query("select * from UserEntity where id in (SELECT userId FROM ConversationEntity where myUid = :myUid)")
    fun getOpenedConversationUsers(myUid: String): List<UserEntity>
}