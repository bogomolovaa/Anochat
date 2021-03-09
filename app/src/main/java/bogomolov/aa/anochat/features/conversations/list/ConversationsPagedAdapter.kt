package bogomolov.aa.anochat.features.conversations.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import bogomolov.aa.anochat.R
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
        return VH(cv, binding)
    }

    override fun getId(item: Conversation) = item.id

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(conversation: Conversation?, holder: VH) {
        val binding = holder.binding
        if (conversation != null) {
            binding.conversation = conversation
            val lastMessage = conversation.lastMessage
            binding.messageText.text =
                if (showFullMessage) {
                    lastMessage?.text ?: ""
                } else {
                    lastMessage?.shortText() ?: ""
                }
            binding.newMessageStatus.visibility =
                if (lastMessage?.isMine == false && lastMessage.viewed == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onItemSelected(binding: ConversationLayoutBinding, selected: Boolean) {
        val context = binding.root.context
        val color = if (selected)
            ContextCompat.getColor(context, R.color.not_my_message_color)
        else
            ContextCompat.getColor(context, R.color.my_message_color)
        binding.cardView.setCardBackgroundColor(color)
        binding.userPhoto.setForegroundColor(color)
    }
}