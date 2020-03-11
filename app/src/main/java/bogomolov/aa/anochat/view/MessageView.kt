package bogomolov.aa.anochat.view

import android.annotation.SuppressLint
import bogomolov.aa.anochat.core.Message
import java.text.SimpleDateFormat
import java.util.*

data class MessageView(val message: Message) {
    var dateDelimiter: String? = null

    fun isTimeMessage() = dateDelimiter != null

    fun hasImage() = message.image != null

    fun hasAudio() = message.audio != null

    fun received() = message.received == 1

    fun viewed() = message.viewed == 1

    fun hasReplyMessageImage() = message.replyMessage?.image != null

    fun hasReplyMessage() = message.replyMessage != null

    fun getReplyText() = message.replyMessage?.text ?: ""

    fun getReplyAudio() = message.replyMessage?.audio

    fun hasReplyAudio() = message.replyMessage?.audio != null

}