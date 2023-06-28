package bogomolov.aa.anochat.domain.entity

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.*

enum class AttachmentStatus {
    LOADING, LOADED, NOT_LOADED
}

@Immutable
data class Message(
    val id: Long = 0L,
    val text: String = "",
    val time: Long = 0L,
    val conversationId: Long = 0L,
    var isMine: Boolean = false,
    val messageId: String = "",
    val replyMessage: Message? = null,
    val replyMessageId: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val video: String? = null,
    val publicKey: String? = null,

    val sent: Int = 0,
    val received: Int = 0,
    val viewed: Int = 0
) {

    fun hasReplyMessage() = replyMessage != null
    fun getReplyText() = replyMessage?.text ?: ""

    fun sent() = sent == 1
    fun received() = received == 1
    fun error() = received == -1
    fun viewed() = viewed == 1
    fun sentAndNotReceived() = sent() && !received() && !error()
    fun receivedAndNotViewed() = received() && !viewed()

    @SuppressLint("SimpleDateFormat")
    fun timeString(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(time))

    @SuppressLint("SimpleDateFormat")
    fun shortTimeString(): String = SimpleDateFormat("HH:mm").format(Date(time))

    fun shortText(): String {
        val suffix = when {
            image != null -> " " + String(Character.toChars(0x1F4F7))
            audio != null -> " " + String(Character.toChars(0x1F50A))
            video != null -> " " + String(Character.toChars(0x1F4F9))
            else -> ""
        }
        return (if (text.length > 30) text.take(30) + "..." else text) + suffix
    }

    fun hasAttachment() = getAttachment() != null

    fun getAttachment() = audio ?: image ?: video

    fun isNotSaved() = id == 0L

    val attachmentStatus
        get() = when {
            (!isMine && hasAttachment() && received == 0) -> AttachmentStatus.LOADING
            (!isMine && hasAttachment() && received == -1) -> AttachmentStatus.NOT_LOADED
            else -> AttachmentStatus.LOADED
        }
}
