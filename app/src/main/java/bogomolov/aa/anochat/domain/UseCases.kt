package bogomolov.aa.anochat.domain

import android.util.Log
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.repositories.*
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

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
    private val crypto: Crypto
) : MessageUseCasesInRepository by messageRep {
    var dispatcher: CoroutineDispatcher = Dispatchers.IO

    suspend fun receiveMessage(message: Message, uid: String, onSuccess: (Message) -> Unit) {
        Log.d(TAG, "receiveMessage $message")
        try {
            val secretKey = crypto.getSecretKey(uid) ?: throw WrongSecretKeyException("null")
            message.text = crypto.decryptString(message.text, secretKey)
            val user = userRep.getOrAddUser(uid)
            message.conversationId = conversationRep.createOrGetConversation(user)
            val replyId = message.replyMessageId
            if (!replyId.isNullOrEmpty()) message.replyMessage = messageRep.getMessage(replyId)
            messageRep.saveMessage(message)
            if (message.hasAttachment())
                tryReceiveAttachment(message, uid, { crypto.decrypt(secretKey, this) }) {
                    onSuccess(message)
                }
            messageRep.notifyAsReceived(message.messageId)
            onSuccess(message)
        } catch (e: WrongSecretKeyException) {
            Log.w(TAG, "not received message $message from $uid: ${e.message} secret key")
            messageRep.notifyAsNotReceived(message.messageId)
            if (keyIsNotSentTo(uid))
                sendPublicKey(crypto.generatePublicKey(uid), uid, initiator = true)
        }
    }

    fun sendMessage(message: Message, uid: String) {
        Log.d(TAG, "sendMessage $message to uid $uid")
        if (message.isNotSaved()) messageRep.saveMessage(message)
        val secretKey = crypto.getSecretKey(uid)
        if (secretKey != null) {
            if (message.hasAttachment())
                GlobalScope.launch(dispatcher) {
                    messageRep.sendAttachment(message, uid) { crypto.encrypt(secretKey, this) }
                }
            message.text = crypto.encryptString(secretKey, message.text)
            messageRep.sendMessage(message, uid)
        } else if (keyIsNotSentTo(uid))
            sendPublicKey(crypto.generatePublicKey(uid), uid, initiator = true)
    }

    fun receivedPublicKey(publicKey: String, uid: String) {
        val myPublicKey = crypto.generatePublicKey(uid)
        generateSecretKey(publicKey, uid)
        sendPublicKey(myPublicKey, uid, initiator = false)
    }

    fun finallyReceivedPublicKey(publicKey: String, uid: String) {
        val generated = generateSecretKey(publicKey, uid)
        if (generated) sendPendingMessages(uid)
    }

    private fun tryReceiveAttachment(
        message: Message,
        uid: String,
        convert: ByteArray.() -> ByteArray,
        onSuccess: () -> Unit
    ) {
        val attempts = 10
        GlobalScope.launch(dispatcher) {
            var counter = 0
            while (counter < attempts) {
                if (messageRep.receiveAttachment(message, uid, convert)) {
                    onSuccess()
                    break
                } else {
                    val wait = 5 * counter.toLong()
                    Log.w(TAG, "attachment not received, wait $wait s")
                    counter++
                    delay(wait * 1000)
                }
            }
        }
    }

    private fun sendPublicKey(publicKey: String?, uid: String, initiator: Boolean) {
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

    private fun sendPendingMessages(uid: String) {
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

    private fun getSentSettingName(uid: String) = "${keyValueStore.getMyUID()!!}${uid}_sent"
}