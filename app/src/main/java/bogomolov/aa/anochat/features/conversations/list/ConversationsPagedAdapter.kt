package bogomolov.aa.anochat.features.conversations.list

import android.view.LayoutInflater
import android.view.ViewGroup
import bogomolov.aa.anochat.databinding.ConversationLayoutBinding
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.ActionModeData
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.ItemClickListener

class ConversationsPagedAdapter(
    private val showFullMessage: Boolean = false,
    actionModeData: ActionModeData<Conversation>? = null,
    onClickListener: ItemClickListener<Conversation>? = null
) : ExtPagedListAdapter<Conversation, ConversationLayoutBinding>(actionModeData, onClickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ConversationLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, cv, binding)
    }

    override fun getId(item: Conversation) = item.id

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(conversation: Conversation?, holder: VH) {
        val binding = holder.binding
        if (conversation != null) {
            binding.conversation = conversation
            binding.messageText.text =
                if (showFullMessage) {
                    conversation.lastMessage?.text ?: ""
                } else {
                    conversation.lastMessage?.shortText() ?: ""
                }
        }
    }
}