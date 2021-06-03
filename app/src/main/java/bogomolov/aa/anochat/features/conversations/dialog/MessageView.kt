package bogomolov.aa.anochat.features.conversations.dialog

import bogomolov.aa.anochat.domain.entity.Message
import java.text.SimpleDateFormat
import java.util.*

data class MessageView(val message: Message) {
    var dateDelimiter: String? = null
    var detailedImageLoaded = false

    fun hasTimeMessage() = dateDelimiter != null
    fun hasReplyMessage() = message.replyMessage != null
    fun getReplyText() = message.replyMessage?.text ?: ""

    fun sent() = message.sent == 1
    fun received() = message.received == 1
    fun error() = message.received == -1
    fun viewed() = message.viewed == 1
    fun sentAndNotReceived() = sent() && !received() && !error()
    fun receivedAndNotViewed() = received() && !viewed()
}

