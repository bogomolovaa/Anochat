package bogomolov.aa.anochat.features.conversations.dialog

import android.graphics.Bitmap
import bogomolov.aa.anochat.domain.entity.Message
import java.text.SimpleDateFormat
import java.util.*

data class MessageViewData(val message: Message) {
    var dateDelimiter: String? = null
    var detailedImageLoaded = false
    var bitmap: Bitmap? = null
    var playingState: PlayingState? = null
    var replyBitmap: Bitmap? = null
    var replyPlayingState: PlayingState? = null

    fun hasTimeMessage() = dateDelimiter != null
    fun hasReplyMessage() = message.replyMessage != null
    fun getReplyText() = message.replyMessage?.text ?: ""

    fun sent() = message.sent == 1
    fun received() = message.received == 1
    fun error() = message.received == -1
    fun viewed() = message.viewed == 1
    fun sentAndNotReceived() = sent() && !received() && !error()
    fun receivedAndNotViewed() = received() && !viewed()

    override fun equals(other: Any?): Boolean {
        return if (other is MessageViewData) {
            message == other.message
                    && bitmap == other.bitmap
                    && playingState == other.playingState
                    && replyBitmap == other.replyBitmap
                    && replyPlayingState == other.replyPlayingState
        } else false
    }

    override fun toString(): String {
        return message.toString() + " playingState $playingState"
    }
}

