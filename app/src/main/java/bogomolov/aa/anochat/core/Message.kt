package bogomolov.aa.anochat.core

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    var id: Long = 0L,
    val text: String,
    val time: Long,
    val conversationId: Long,
    val senderId: Long = 0L
){
    @SuppressLint("SimpleDateFormat")
    fun timeString():String = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(time))
}
