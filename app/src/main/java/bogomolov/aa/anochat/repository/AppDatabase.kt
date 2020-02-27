package bogomolov.aa.anochat.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import bogomolov.aa.anochat.repository.dao.ConversationDao
import bogomolov.aa.anochat.repository.dao.MessageDao
import bogomolov.aa.anochat.repository.dao.UserDao
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import bogomolov.aa.anochat.repository.entity.MessageEntity
import bogomolov.aa.anochat.repository.entity.UserEntity

const val DB_NAME = "anochat_db"

@Database(entities = [ConversationEntity::class, MessageEntity::class, UserEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao

    abstract fun userDao(): UserDao


}