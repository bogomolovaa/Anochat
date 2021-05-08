package bogomolov.aa.anochat.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import bogomolov.aa.anochat.repository.dao.ConversationDao
import bogomolov.aa.anochat.repository.dao.MessageDao
import bogomolov.aa.anochat.repository.dao.UserDao
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import bogomolov.aa.anochat.repository.entity.MessageEntity
import bogomolov.aa.anochat.repository.entity.UserEntity

const val DB_NAME = "anochat_db"

@Database(entities = [ConversationEntity::class, MessageEntity::class, UserEntity::class], exportSchema = false, version = 61)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao

    abstract fun userDao(): UserDao


}

val MIGRATION = object : Migration(60, 61) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE MessageEntity ADD COLUMN video TEXT")
    }
}