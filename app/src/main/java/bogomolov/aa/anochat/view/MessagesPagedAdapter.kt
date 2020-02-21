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

class MessagesPagedAdapter : PagedListAdapter<Message, MessagesPagedAdapter.MessageViewHolder>(DIFF_CALLBACK) {

    val selectedIds: MutableSet<Long> = HashSet()
    private var checkMode = false


    fun disableCheckMode() {
        selectedIds.clear()
        checkMode = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.trackCardView
        return MessageViewHolder(cv, binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        val cardView = holder.cardView as MaterialCardView
        if (message != null) {
            val selected = selectedIds.contains(message.id)
            cardView.isChecked = selected
        }
        holder.bind(message)
    }

    inner class MessageViewHolder(val cardView: CardView, val binding: MessageLayoutBinding) :
        RecyclerView.ViewHolder(cardView), View.OnClickListener, View.OnLongClickListener {
        init {
            cardView.setOnClickListener(this)
            cardView.setOnLongClickListener(this)
        }

        fun bind(message: Message?) {
            //binding.
        }

        override fun onClick(v: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            val position = adapterPosition
            val adapter = this@MessagesPagedAdapter;
            val message = adapter.getItem(position)
            if (message != null) {
                if (adapter.checkMode) {
                    if (adapter.selectedIds.contains(message.id)) {
                        adapter.selectedIds.remove(message.id)
                    } else {
                        adapter.selectedIds.add(message.id)
                    }
                    adapter.notifyItemChanged(position)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            val adapter = this@MessagesPagedAdapter;
            adapter.checkMode = true
            adapter.selectedIds!!.clear()
            onClick(view)
            adapter.notifyDataSetChanged()
            //adapter.tracksListFragment.onLongClick()
            return true
        }
    }
}

