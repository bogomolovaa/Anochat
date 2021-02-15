package bogomolov.aa.anochat.domain

import android.util.Log
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.repository.repositories.ConversationRepository
import bogomolov.aa.anochat.repository.repositories.MessageRepository
import bogomolov.aa.anochat.repository.repositories.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MessageUseCases"

@Singleton
class UseCases @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val crypto: Crypto
) {

    suspend fun receiveMessage(
        text: String,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?
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
                messageRepository.receiveAttachment(fileName, uid, localFile)
                crypto.decryptFile(localFile, secretKey)
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

    suspend fun sendMessage(message: Message, uid: String) {
        Log.i("test", "sendMessage message")
        if (message.isNotSaved()) messageRepository.saveMessage(message)
        val secretKey = crypto.getSecretKey(uid)
        if (secretKey != null) {
            val fileName = message.audio ?: message.image
            if (fileName != null) {
                val localFile = messageRepository.getAttachmentFile(fileName)
                val byteArray = crypto.encryptFile(localFile, secretKey)
                messageRepository.sendAttachment(fileName, uid, byteArray)
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

    suspend fun finallyReceivedPublicKey(publicKey: String, uid: String) {
        val generated = generateSecretKey(publicKey, uid)
        if (generated) sendPendingMessages(uid)
    }




    private fun sendPublicKey(uid: String, initiator: Boolean) {
        if (messageRepository.keyIsNotSentTo(uid)) {
            val publicKey = crypto.generatePublicKey(uid)
            if (publicKey != null) {
                messageRepository.sendPublicKey(publicKey, uid, initiator)
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
            messageRepository.setKeyAsSentTo(uid)
        } else {
            Log.i(TAG, "secret key not generated: privateKey null")
        }
        return generated
    }

    private suspend fun sendPendingMessages(uid: String) {
        for (message in messageRepository.getPendingMessages(uid)) sendMessage(message, uid)
    }
}