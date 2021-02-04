package bogomolov.aa.anochat.features.conversations.dialog

import bogomolov.aa.anochat.domain.Message

data class MessageView(val message: Message) {
    var dateDelimiter: String? = null

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