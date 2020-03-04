package bogomolov.aa.anochat.core

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    var id: Long = 0L,
    val text: String,
    val time: Long = 0L,
    val conversationId: Long = 0L,
    val senderId: Long = 0L,
    val image: String? = null
) {
    @SuppressLint("SimpleDateFormat")
    fun timeString(): String = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(time))

    @SuppressLint("SimpleDateFormat")
    fun shortTimeString(): String = SimpleDateFormat("HH:mm").format(Date(time))

    fun shortText(): String = if (text.length > 30) text.take(30) + "..." else text

}
