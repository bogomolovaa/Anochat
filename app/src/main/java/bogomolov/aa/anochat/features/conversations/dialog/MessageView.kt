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

fun insertDateSeparators(message1: MessageView?, message2: MessageView?, locale: Locale) {
    if (message1 != null && message2 != null) {
        val day1 = GregorianCalendar().apply { time = Date(message1.message.time) }
            .get(Calendar.DAY_OF_YEAR)
        val day2 = GregorianCalendar().apply { time = Date(message2.message.time) }
            .get(Calendar.DAY_OF_YEAR)
        if (day1 != day2)
            message1.dateDelimiter =
                SimpleDateFormat("dd MMMM yyyy", locale).format(Date(message1.message.time))
    }
}