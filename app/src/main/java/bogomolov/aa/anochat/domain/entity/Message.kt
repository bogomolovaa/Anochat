package bogomolov.aa.anochat.domain.entity

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

enum class AttachmentStatus {
    LOADING, LOADED, NOT_LOADED
}

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

    @SuppressLint("SimpleDateFormat")
    fun timeString(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(time))

    @SuppressLint("SimpleDateFormat")
    fun shortTimeString(): String = SimpleDateFormat("HH:mm").format(Date(time))

    fun shortText(): String {
        if (image != null) return String(Character.toChars(0x1F4F7))
        if (audio != null) return String(Character.toChars(0x1F50A))
        if (video != null) return String(Character.toChars(0x1F4F9))
        return if (text.length > 30) text.take(30) + "..." else text
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
