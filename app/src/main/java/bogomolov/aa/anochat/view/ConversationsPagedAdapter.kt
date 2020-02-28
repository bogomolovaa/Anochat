package bogomolov.aa.anochat.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.databinding.ConversationLayoutBinding
import com.google.android.material.card.MaterialCardView

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Conversation>() {
    override fun areItemsTheSame(
        conversation1: Conversation,
        conversation2: Conversation
    ): Boolean =
        conversation1 == conversation2

    override fun areContentsTheSame(
        conversation1: Conversation,
        conversation2: Conversation
    ): Boolean =
        conversation1 == conversation2

}

class ConversationsPagedAdapter :
    PagedListAdapter<Conversation, ConversationsPagedAdapter.ConversationViewHolder>(DIFF_CALLBACK) {

    //val selectedIds: MutableSet<Long> = HashSet()
    //private var checkMode = false


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding =
            ConversationLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.messageCardView
        return ConversationViewHolder(cv, binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = getItem(position)
        //val cardView = holder.cardView as MaterialCardView
        //if (conversation != null) cardView.isChecked = selectedIds.contains(conversation.id)
        holder.bind(conversation)
    }

    inner class ConversationViewHolder(
        val cardView: CardView,
        val binding: ConversationLayoutBinding
    ) : RecyclerView.ViewHolder(cardView) {
        fun bind(conversation: Conversation?) {
            if (conversation != null)
                binding.conversation = conversation
        }
    }
}