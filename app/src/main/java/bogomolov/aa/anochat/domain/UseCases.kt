package bogomolov.aa.anochat.domain

import android.util.Log
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.repositories.*
import bogomolov.aa.anochat.repository.NotificationsService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton


interface MessagesListener {
    suspend fun onMessageReceived(message: Message, uid: String)
    suspend fun onReportReceived(messageId: String, received: Int, viewed: Int)
    suspend fun onPublicKeyReceived(uid: String, publicKey: String, initiator: Boolean)
}

@Singleton
open class UserUseCases @Inject constructor(private val userRepository: UserRepository) :
    UserUseCasesInRepository by userRepository

@Singleton
open class ConversationUseCases @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) : ConversationUseCasesInRepository by conversationRepository {

    suspend fun startConversation(uid: String): Long {
        val user = userRepository.getOrAddUser(uid)
        return conversationRepository.createOrGetConversation(user)
    }
}

private const val TAG = "MessageUseCases"

@Singleton
open class MessageUseCases @Inject constructor(
    private val messageRep: MessageRepository,
    private val conversationRep: ConversationRepository,
    private val userRep: UserRepository,
    private val keyValueStore: KeyValueStore,
    private val crypto: Crypto,
    private val notificationsService: NotificationsService
) : MessageUseCasesInRepository by messageRep {

    val messagesListener = object : MessagesListener {
        override suspend fun onMessageReceived(message: Message, uid: String) {
            receiveMessage(message, uid)?.let {
                notificationsService.showNotification(it)
            }
        }

        override suspend fun onReportReceived(messageId: String, received: Int, viewed: Int) {
            receiveReport(messageId, received, viewed)
        }

        override suspend fun onPublicKeyReceived(uid: String, publicKey: String, initiator: Boolean) {
            if (initiator) {
                Log.d(TAG, "receivedPublicKey from $uid")
                receivedPublicKey(publicKey, uid)
            } else {
                Log.d(TAG, "finallyReceivedPublicKey from $uid")
                finallyReceivedPublicKey(publicKey, uid)
            }
        }
    }

    suspend fun receiveMessage(message: Message, uid: String): Message? {
        if (messageRep.getMessage(message.messageId) != null) return null
        Log.d(TAG, "receiveMessage $message")
        try {
            val secretKey = crypto.getSecretKey(uid) ?: throw WrongSecretKeyException("null")
            messageRep.notifyAsReceived(message.messageId, uid)
            return message.copy(
                text = crypto.decryptString(message.text, secretKey),
                conversationId = conversationRep.createOrGetConversation(userRep.getOrAddUser(uid)),
                replyMessage = message.replyMessageId?.let { messageRep.getMessage(it) },
            ).run { copy(id = messageRep.saveMessage(this)) }
        } catch (e: WrongSecretKeyException) {
            Log.w(TAG, "not received message $message from $uid: ${e.message} secret key")
            messageRep.notifyAsNotReceived(message.messageId, uid)
            if (keyIsNotSentTo(uid))
                sendPublicKey(crypto.generatePublicKey(uid), uid, initiator = true)
        }
        return null
    }

    suspend fun sendMessage(message: Message, uid: String) = coroutineScope {
        val id = if (message.isNotSaved()) messageRep.saveMessage(message) else message.id
        val secretKey = crypto.getSecretKey(uid)
        Log.d(TAG, "sendMessage $message to uid $uid secretKey $secretKey")
        if (secretKey != null) {
            message.getAttachment()?.let { fileName ->
                launch {
                    messageRep.runAttachmentUploading(fileName, uid)
                }
            }
            val text = crypto.encryptString(secretKey, message.text)
            messageRep.sendMessage(message.copy(id = id, text = text), uid)
        } else if (keyIsNotSentTo(uid))
            sendPublicKey(crypto.generatePublicKey(uid), uid, initiator = true)
    }

    suspend fun receivedPublicKey(publicKey: String, uid: String) {
        val myPublicKey = crypto.generatePublicKey(uid)
        generateSecretKey(publicKey, uid)
        sendPublicKey(myPublicKey, uid, initiator = false)
    }

    suspend fun finallyReceivedPublicKey(publicKey: String, uid: String) {
        val generated = generateSecretKey(publicKey, uid)
        if (generated) sendPendingMessages(uid)
    }

    fun loadMessagesDataSource(conversationId: Long) =
        messageRep.loadMessagesDataSource(conversationId)

    suspend fun messageDisplayed(message: Message, uid: String) {
        if (!message.isMine && message.viewed == 0) {
            messageRep.notifyAsViewed(message, uid)
        }
        if (!message.isMine && message.hasAttachment() && message.received == 0) {
            crypto.getSecretKey(uid)?.let { secretKey ->
                messageRep.receiveAttachment(message, uid) {
                    crypto.decrypt(secretKey, this)
                }
            }
        }
    }

    private suspend fun sendPublicKey(publicKey: String?, uid: String, initiator: Boolean) {
        if (publicKey != null) {
            Log.d(TAG, "send publicKey for $uid")
            messageRep.sendPublicKey(publicKey, uid, initiator)
            setKeyAsSent(uid)
        } else {
            Log.d(TAG, "publicKey not generated for $uid")
        }
    }

    private fun generateSecretKey(publicKey: String, uid: String): Boolean {
        val secretKey = crypto.generateSecretKey(publicKey, uid)
        val generated = secretKey != null
        if (generated) {
            Log.d(TAG, "secret key generated for $uid")
            resetKeyAsSent(uid)
        } else {
            Log.i(TAG, "secret key not generated for $uid: privateKey null")
        }
        return generated
    }

    private suspend fun sendPendingMessages(uid: String) {
        for (message in messageRep.getPendingMessages(uid)) sendMessage(message, uid)
    }

    private fun keyIsNotSentTo(uid: String): Boolean {
        val sentSettingName = getSentSettingName(uid)
        return !(keyValueStore.getValue<Boolean>(sentSettingName) ?: false)
    }

    private fun resetKeyAsSent(uid: String) {
        val sentSettingName = getSentSettingName(uid)
        keyValueStore.setValue(sentSettingName, false)
    }

    private fun setKeyAsSent(uid: String) {
        val sentSettingName = getSentSettingName(uid)
        keyValueStore.setValue(sentSettingName, true)
    }

    private fun getSentSettingName(uid: String) = "${keyValueStore.getMyUID()}_${uid}_sent"
}