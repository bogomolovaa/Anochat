package bogomolov.aa.anochat.features.conversations.dialog

import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import bogomolov.aa.anochat.domain.entity.Message
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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

    companion object{
        fun toMessageViewsWithDateDelimiters(messages: List<Message>, locale: Locale): List<MessageView> {
            val list: MutableList<MessageView> = ArrayList()
            //if (messages != null) {
            var lastDay = -1
            for ((i, message) in messages.listIterator().withIndex()) {
                val messageView = MessageView(message)
                val day = GregorianCalendar().apply { time = Date(message.time) }
                    .get(Calendar.DAY_OF_YEAR)
                if (i > 0) {
                    if (lastDay != day) {
                        val dateString =
                            SimpleDateFormat(
                                "dd MMMM yyyy",
                                locale
                            ).format(Date(message.time))
                        messageView.dateDelimiter = dateString
                    }
                }
                lastDay = day
                list.add(messageView)
            }
            //}
            return list
        }
    }
}

@BindingAdapter(value = ["android:layout_marginLeft", "android:layout_marginRight"])
fun setLayoutMargin(view: MaterialCardView, marginLeft: Float, marginRight: Float) {
    val p = view.layoutParams as ViewGroup.MarginLayoutParams
    p.setMargins(marginLeft.toInt(), p.topMargin, marginRight.toInt(), p.bottomMargin);
    view.requestLayout()
}