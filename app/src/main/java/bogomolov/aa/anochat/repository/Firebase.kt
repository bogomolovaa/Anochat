package bogomolov.aa.anochat.repository

import bogomolov.aa.anochat.domain.MessagesListener
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import kotlinx.coroutines.flow.Flow

interface Firebase {
    fun sendTyping(myUid: String, uid: String, started: Int)
    suspend fun updateToken(): String?
    fun setOffline()
    fun setOnline()
    suspend fun addUserStatusListener(
        myUid: String,
        uid: String
    ): Flow<Triple<Boolean, Boolean, Long>>

    suspend fun findByPhone(phone: String): List<User>
    suspend fun getUser(uid: String): User?
    fun renameUser(uid: String, name: String)
    fun updateStatus(uid: String, status: String?)
    fun updatePhoto(uid: String, photo: String)
    suspend fun receiveUsersByPhones(phones: List<String>): List<User>

    fun setMessagesListener(listener: MessagesListener)

    fun removeMessagesListener()

    suspend fun sendMessage(
        message: Message?,
        uid: String
    ): Pair<String, Long>?

    suspend fun sendReport(
        messageId: String,
        uid: String,
        received: Int,
        viewed: Int
    )

    suspend fun sendKey(
        uid: String,
        publicKey: String?,
        initiator: Boolean
    )

    suspend fun uploadFile(
        fileName: String,
        uid: String,
        byteArray: ByteArray,
        isPrivate: Boolean = false
    ): Boolean

    suspend fun downloadFile(
        fileName: String,
        uid: String,
        isPrivate: Boolean = false
    ): ByteArray?

    fun deleteRemoteMessage(messageId: String)
}

