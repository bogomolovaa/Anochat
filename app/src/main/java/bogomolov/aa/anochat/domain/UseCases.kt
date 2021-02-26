package bogomolov.aa.anochat.domain

import android.util.Log
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
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
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val keyValueStore: KeyValueStore,
    private val crypto: Crypto
) : MessageUseCasesInRepository by messageRepository {

    suspend fun receiveMessage(
        text: String,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?,
        onAttachmentReceived: (Message) -> Unit
    ): Message? {
        val secretKey = crypto.getSecretKey(uid)
        return if (secretKey != null) {
            Log.i(TAG, "receiveMessage $messageId $text replyId $replyId")
            val user = userRepository.getOrAddUser(uid)
            val conversationId = conversationRepository.createOrGetConversation(user)
            val message = Message(
                time = System.currentTimeMillis(),
                conversationId = conversationId,
                messageId = messageId,
                image = image,
                audio = audio
            )
            if (text.isNotEmpty()) message.text = crypto.decryptString(text, secretKey)
            if (!replyId.isNullOrEmpty()) message.replyMessage =
                messageRepository.getMessage(replyId)
            val fileName = message.audio ?: message.image
            if (fileName != null) {
                val localFile = messageRepository.getAttachmentFile(fileName)
                tryReceiveAttachment(fileName, localFile, uid) {
                    crypto.decryptFile(localFile, secretKey)
                    messageRepository.updateAsReceived(message)
                    onAttachmentReceived(message)
                }
            }
            messageRepository.saveMessage(message)
            messageRepository.notifyAsReceived(messageId)
            message
        } else {
            Log.i(TAG, "not received message $messageId from $uid: null secretKey")
            messageRepository.notifyAsNotReceived(messageId)
            sendPublicKey(uid, initiator = true)
            null
        }
    }

    fun sendMessage(message: Message, uid: String) {
        Log.i("test", "sendMessage message")
        if (message.isNotSaved()) messageRepository.saveMessage(message)
        val secretKey = crypto.getSecretKey(uid)
        if (secretKey != null) {
            val fileName = message.audio ?: message.image
            if (fileName != null) {
                val localFile = messageRepository.getAttachmentFile(fileName)
                val byteArray = crypto.encryptFile(localFile, secretKey)
                GlobalScope.launch(Dispatchers.IO) {
                    messageRepository.sendAttachment(fileName, uid, byteArray)
                }
            }
            message.text = crypto.encryptString(secretKey, message.text)
            messageRepository.sendMessage(message, uid)
        } else {
            sendPublicKey(uid, initiator = true)
        }
    }

    fun receivedPublicKey(publicKey: String, uid: String) {
        sendPublicKey(uid, initiator = false)
        generateSecretKey(publicKey, uid)
    }

    fun finallyReceivedPublicKey(publicKey: String, uid: String) {
        val generated = generateSecretKey(publicKey, uid)
        if (generated) sendPendingMessages(uid)
    }


    private fun tryReceiveAttachment(
        fileName: String,
        localFile: File,
        uid: String,
        onSuccess: () -> Unit
    ) {
        val attempts = 10
        GlobalScope.launch(Dispatchers.IO) {
            var counter = 0
            while (counter < attempts) {
                if (messageRepository.receiveAttachment(fileName, uid, localFile)) {
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

    private fun sendPublicKey(uid: String, initiator: Boolean) {
        if (keyIsNotSentTo(uid)) {
            val publicKey = crypto.generatePublicKey(uid)
            if (publicKey != null) {
                messageRepository.sendPublicKey(publicKey, uid, initiator)
                setKeyAsSent(uid)
                Log.i(TAG, "send publicKey for $uid")
            } else {
                Log.i(TAG, "publicKey not generated for $uid")
            }
        }
    }

    private fun generateSecretKey(publicKey: String, uid: String): Boolean {
        val generated = crypto.generateSecretKey(publicKey, uid)
        if (generated) {
            Log.i(TAG, "secret key generated")
            resetKeyAsSent(uid)
        } else {
            Log.i(TAG, "secret key not generated: privateKey null")
        }
        return generated
    }

    private fun sendPendingMessages(uid: String) {
        for (message in messageRepository.getPendingMessages(uid)) sendMessage(message, uid)
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