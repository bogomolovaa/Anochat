package bogomolov.aa.anochat.view

import android.annotation.SuppressLint
import bogomolov.aa.anochat.core.Message
import java.text.SimpleDateFormat
import java.util.*

data class MessageView (val message: Message){
    var dateDelimiter: String? = null

    fun isMine() = message.senderId == 0L

    fun isTimeMessage() = dateDelimiter != null

    fun hasImage() = message.image != null
}