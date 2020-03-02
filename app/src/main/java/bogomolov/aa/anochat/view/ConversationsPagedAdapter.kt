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

class ConversationsPagedAdapter(private val helper: AdapterHelper<Conversation, ConversationLayoutBinding> = AdapterHelper()) :
    PagedListAdapter<Conversation, AdapterHelper<Conversation, ConversationLayoutBinding>.VH>(
        helper.DIFF_CALLBACK
    ), AdapterSelectable<Conversation, ConversationLayoutBinding> {

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<Conversation, ConversationLayoutBinding>.VH {
        val binding =
            ConversationLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.messageCardView
        return helper.VH(cv, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<Conversation, ConversationLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = super.getItem(position)

    override fun getId(item: Conversation) = item.id

    override fun bind(item: Conversation?, binding: ConversationLayoutBinding) {
        if (item != null)
            binding.conversation = item
    }

}