package bogomolov.aa.anochat.features.conversations.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import bogomolov.aa.anochat.databinding.ConversationLayoutBinding
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.view.adapters.AdapterHelper
import bogomolov.aa.anochat.view.adapters.AdapterSelectable

class ConversationsPagedAdapter(
    private val showFullMessage: Boolean = false,
    private val helper: AdapterHelper<Conversation, ConversationLayoutBinding> = AdapterHelper()
) :
    PagedListAdapter<Conversation, AdapterHelper<Conversation, ConversationLayoutBinding>.VH>(
        helper.DIFF_CALLBACK
    ),
    AdapterSelectable<Conversation, ConversationLayoutBinding> {

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<Conversation, ConversationLayoutBinding>.VH {
        val binding =
            ConversationLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return helper.VH(cv, cv, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<Conversation, ConversationLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = super.getItem(position)

    override fun getId(item: Conversation) = item.id

    override fun bind(item: Conversation?, binding: ConversationLayoutBinding) {
        if (item != null) {
            binding.conversation = item
            binding.messageText.text =
                if (showFullMessage) {
                    item.lastMessage?.text ?: ""
                } else {
                    item.lastMessage?.shortText() ?: ""
                }
        }
    }

}