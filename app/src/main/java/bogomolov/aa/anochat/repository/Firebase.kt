package bogomolov.aa.anochat.repository

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

    fun sendReport(messageId: String, received: Int, viewed: Int)
    fun sendMessage(
        message: Message? = null,
        uid: String,
        publicKey: String? = null,
        initiator: Boolean = false,
        onSuccess: () -> Unit = {}
    ): String

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

