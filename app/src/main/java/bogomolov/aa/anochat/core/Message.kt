package bogomolov.aa.anochat.core

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    var id: Long = 0L,
    val text: String = "",
    val time: Long = 0L,
    val conversationId: Long = 0L,
    val senderId: Long = 0L,
    var messageId: String = "",
    val replyMessage: Message? = null,
    val image: String? = null,
    val audio: String? = null,
    var publicKey: String? = null,
    var sent: Int = 0,
    var received: Int = 0,
    var viewed: Int = 0
) {
    @SuppressLint("SimpleDateFormat")
    fun timeString(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(time))

    @SuppressLint("SimpleDateFormat")
    fun shortTimeString(): String = SimpleDateFormat("HH:mm").format(Date(time))

    fun shortText(): String = if (text.length > 30) text.take(30) + "..." else text

    fun isMine() = senderId == 0L

}
