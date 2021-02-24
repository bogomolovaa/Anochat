package bogomolov.aa.anochat.features.conversations.dialog

import bogomolov.aa.anochat.domain.entity.Message
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

data class MessageView(val message: Message) {
    var dateDelimiter: String? = null
    var detailedImageLoaded = false

    fun isTimeMessage() = dateDelimiter != null
    fun hasImage() = message.image != null
    fun hasAudio() = message.audio != null
    fun error() = message.received == -1
    fun received() = message.received == 1
    fun viewed() = message.viewed == 1
    fun hasReplyMessageImage() = message.replyMessage?.image != null
    fun hasReplyMessage() = message.replyMessage != null
    fun getReplyText() = message.replyMessage?.text ?: ""
    fun getReplyAudio() = message.replyMessage?.audio
    fun hasReplyAudio() = message.replyMessage?.audio != null
}

fun toMessageViewsWithDateDelimiters(messages: List<Message>, locale: Locale): List<MessageView> {
    val list = ArrayList<MessageView>()
    var lastDay = -1
    val calendar = GregorianCalendar()
    for ((i, message) in messages.listIterator().withIndex()) {
        val messageView = MessageView(message)
        calendar.time = Date(message.time)
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        if (i > 0 && lastDay != day)
            messageView.dateDelimiter =
                SimpleDateFormat("dd MMMM yyyy", locale).format(Date(message.time))
        lastDay = day
        list.add(messageView)
    }
    return list
}