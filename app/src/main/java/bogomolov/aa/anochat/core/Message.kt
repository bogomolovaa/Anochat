package bogomolov.aa.anochat.core

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    var id: Long = 0L,
    val text: String,
    val time: Long = 0L,
    val conversationId: Long = 0L,
    val senderId: Long = 0L
) {
    var dateDelimiter: String? = null

    @SuppressLint("SimpleDateFormat")
    fun timeString(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(time))

    @SuppressLint("SimpleDateFormat")
    fun shortTimeString(): String = SimpleDateFormat("HH:mm").format(Date(time))

    fun shortText(): String = text.take(20)

    fun isMine() = senderId == 0L

    fun isTimeMessage() = dateDelimiter != null
}
