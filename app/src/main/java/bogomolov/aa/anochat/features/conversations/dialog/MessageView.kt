package bogomolov.aa.anochat.features.conversations.dialog

import bogomolov.aa.anochat.domain.entity.Message
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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

fun toMessageViewsWithDateDelimiters(messages: List<Message>, locale: Locale): List<MessageView> {
    val list = ArrayList<MessageView>()
    var lastDay = -1
    val calendar = GregorianCalendar()
    for ((i, message) in messages.reversed().withIndex()) {
        val messageView = MessageView(message)
        calendar.time = Date(message.time)
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        if (i > 0 && lastDay != day)
            messageView.dateDelimiter =
                SimpleDateFormat("dd MMMM yyyy", locale).format(Date(message.time))
        lastDay = day
        list.add(messageView)
    }
    return list.reversed()
}