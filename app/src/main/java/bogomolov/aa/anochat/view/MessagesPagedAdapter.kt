package bogomolov.aa.anochat.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.databinding.MessageLayoutBinding
import com.google.android.material.card.MaterialCardView

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(message1: Message, message2: Message): Boolean =
        message1 == message2

    override fun areContentsTheSame(message1: Message, message2: Message): Boolean =
        message1 == message2

}

class MessagesPagedAdapter :
    PagedListAdapter<Message, MessagesPagedAdapter.MessageViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding =
            MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.messageCardView
        return MessageViewHolder(cv, binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        val cardView = holder.cardView as MaterialCardView
        holder.bind(message)
    }

    inner class MessageViewHolder(val cardView: CardView, val binding: MessageLayoutBinding) :
        RecyclerView.ViewHolder(cardView) {


        fun bind(message: Message?) {
            if (message != null) {
                if (message.senderId == 0L) {
                    binding.messageTextMy.text = message.text
                } else {
                    binding.messageTextNotMy.text = message.text
                }
            }
        }


    }
}

class MessagesPagedAdapter2(val helper: RecyclerAdapterHelper<Message>) :
    PagedListAdapter<Message, RecyclerAdapterHelper<Message>.HelperViewHolder>(DIFF_CALLBACK),
    AdapterWithViewHolder<Message> by helper

