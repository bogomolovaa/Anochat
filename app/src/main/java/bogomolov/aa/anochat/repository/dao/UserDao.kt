package bogomolov.aa.anochat.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import bogomolov.aa.anochat.repository.entity.UserEntity

@Dao
interface UserDao {

    @Insert
    fun add(user: UserEntity): Long

    @Query("select * from UserEntity where id = :id")
    fun getUser(id: Long): UserEntity?

    @Query("select * from UserEntity where uid = :uid")
    fun findByUid(uid: String): UserEntity?

    @Query("update UserEntity set photo = :photo where uid = :uid")
    fun updatePhoto(photo: String?, uid: String)

    @Query("update UserEntity set name = :name where uid = :uid")
    fun updateName(name: String?, uid: String)

}