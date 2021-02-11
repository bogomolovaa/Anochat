package bogomolov.aa.anochat.repository

import android.content.Context
import androidx.core.content.edit
import androidx.paging.DataSource
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.conversations.Crypto
import kotlinx.coroutines.CoroutineScope


interface Repository : IFirebaseRepository {

    suspend fun updateUsersByPhones(phones: List<String>): List<User>

    suspend fun updateUsersInConversations()

    suspend fun getMyUser(): User

    fun getUser(id: Long): User

    suspend fun updateMyUser(user: User)

    suspend fun searchByPhone(phone: String): List<User>




    fun getConversation(id: Long): Conversation

    suspend fun createConversation(user: User): Long

    suspend fun deleteConversations(ids: Set<Long>)

    suspend fun deleteConversationIfNoMessages(conversationId: Long)




    suspend fun receiveMessage(
        text: String,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?
    ): Message?

    suspend fun sendMessage(message: Message, uid: String)

    suspend fun sendPendingMessages(uid: String)

    suspend fun deleteMessages(ids: Set<Long>)




    fun generateSecretKey(publicKey: String, uid: String): Boolean

    fun receiveReport(messageId: String, received: Int, viewed: Int)

    fun sendPublicKey(uid: String, initiator: Boolean)




    fun getImagesDataSource(userId: Long): DataSource.Factory<Int, String>

    fun getUsersByPhonesDataSource(phones: List<String>): DataSource.Factory<Int, User>

    fun searchMessagesDataSource(search: String): DataSource.Factory<Int, Conversation>

    fun loadConversationsDataSource(): DataSource.Factory<Int, Conversation>

    fun loadMessagesDataSource(conversationId: Long, scope: CoroutineScope): DataSource.Factory<Int, Message>

    fun getContext(): Context

}

class Setting {
    companion object {
        const val NOTIFICATIONS = "notifications"
        const val SOUND = "sound"
        const val VIBRATION = "vibration"
        const val UID = "uid"
    }
}


inline fun <reified T> Repository.getSetting(setting: String): T? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext())
    return when (T::class) {
        ByteArray::class -> {
            val value = sharedPreferences.getString(setting, null)
            if (value != null) Crypto.base64ToByteArray(value) as T? else null
        }
        String::class -> sharedPreferences.getString(setting, null) as T?
        Boolean::class -> sharedPreferences.getBoolean(setting, false) as T
        else -> null
    }
}

inline fun <reified T> Repository.setSetting(setting: String, value: T?) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(getContext())
    when (T::class) {
        ByteArray::class -> {
            val string = Crypto.byteArrayToBase64(value as ByteArray)
            preferences.edit(true) { putString(setting, string) }
        }
        String::class -> preferences.edit(true) { putString(setting, value as String?) }
        Boolean::class -> preferences.edit(true) { putBoolean(setting, value as Boolean) }
    }
}

fun Repository.getMyUID() = getSetting<String>(Setting.UID)

fun Repository.getSentSettingName(uid: String) = "${getMyUID()!!}${uid}_sent"