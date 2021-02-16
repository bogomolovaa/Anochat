package bogomolov.aa.anochat.repository

import bogomolov.aa.anochat.domain.entity.User
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.io.File

interface Firebase {
    fun signUp(name: String, email: String, password: String): Boolean

    suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): String?
    fun signOut()
    fun isSignedIn(): Boolean
    fun addUserStatusListener(
        uid: String,
        scope: CoroutineScope
    ): Flow<Pair<Boolean, Long>>

    suspend fun findByPhone(phone: String): List<User>

    suspend fun receiveUsersByPhones(phones: List<String>): List<User>
    fun sendReport(messageId: String, received: Int, viewed: Int)
    fun sendMessage(
        text: String? = null,
        replyId: String? = null,
        image: String? = null,
        audio: String? = null,
        uid: String,
        publicKey: String? = null,
        initiator: Boolean = false
    ): String

    fun renameUser(uid: String, name: String)
    fun updateStatus(uid: String, status: String?)
    fun updatePhoto(uid: String, photo: String)

    suspend fun uploadFile(
        fileName: String,
        uid: String,
        byteArray: ByteArray,
        isPrivate: Boolean = false
    ): Boolean

    suspend fun downloadFile(
        fileName: String,
        uid: String,
        localFile: File,
        isPrivate: Boolean = false
    ): Boolean

    fun deleteRemoteMessage(messageId: String)

    suspend fun getUser(uid: String): User?
}

