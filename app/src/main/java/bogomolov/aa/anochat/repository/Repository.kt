package bogomolov.aa.anochat.repository

import androidx.paging.DataSource
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.domain.Settings
import bogomolov.aa.anochat.domain.User
import kotlinx.coroutines.CoroutineScope

const val UID = "uid"

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

    suspend fun deleteConversationIfNoMessages(conversation: Conversation)


    suspend fun receiveMessage(
        text: String,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?
    ): Message?

    suspend fun sendMessage(message: Message, uid: String)

    suspend fun deleteMessages(ids: Set<Long>)


    fun updateSettings(settings: Settings)

    fun getSettings(): Settings


    fun generateSecretKey(publicKey: String, uid: String): Boolean

    fun sendPublicKey(uid: String, initiator: Boolean)

    suspend fun sendPendingMessages(uid: String)

    fun receiveReport(messageId: String, received: Int, viewed: Int)



    fun getImagesDataSource(userId: Long): DataSource.Factory<Int, String>

    fun getUsersByPhonesDataSource(phones: List<String>): DataSource.Factory<Int, User>

    fun searchMessagesDataSource(search: String): DataSource.Factory<Int, Conversation>

    fun loadConversationsDataSource(): DataSource.Factory<Int, Conversation>

    fun loadMessagesDataSource(
        conversationId: Long,
        scope: CoroutineScope
    ): DataSource.Factory<Int, Message>
}