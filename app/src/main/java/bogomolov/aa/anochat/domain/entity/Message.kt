package bogomolov.aa.anochat.domain.entity

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    var id: Long = 0L,
    var text: String = "",
    var time: Long = 0L,
    var conversationId: Long = 0L,
    var isMine: Boolean = false,
    var messageId: String = "",
    var replyMessage: Message? = null,
    var replyMessageId: String? = null,
    var image: String? = null,
    var audio: String? = null,
    var publicKey: String? = null,

    var sent: Int = 0,
    var received: Int = 0,
    var viewed: Int = 0
) {

    override fun hashCode(): Int = id.toString().hashCode()

    @SuppressLint("SimpleDateFormat")
    fun timeString(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(time))

    @SuppressLint("SimpleDateFormat")
    fun shortTimeString(): String = SimpleDateFormat("HH:mm").format(Date(time))

    fun shortText(): String {
        if (image != null) return String(Character.toChars(0x1F4F7))
        if (audio != null) return String(Character.toChars(0x1F3A4))
        return if (text.length > 30) text.take(30) + "..." else text
    }

    fun hasAttachment() = getAttachment() != null

    fun getAttachment() = audio ?: image

    fun isNotSaved() = id == 0L

}
