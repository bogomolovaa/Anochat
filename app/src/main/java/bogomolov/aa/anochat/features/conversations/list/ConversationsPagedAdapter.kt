package bogomolov.aa.anochat.features.conversations.list

import android.graphics.Typeface
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
            val context = binding.cardView.context
            binding.userName.text = conversation.user.name
            if (conversation.user.photo != null) binding.userPhoto.setImage(conversation.user.photo)
            val lastMessage = conversation.lastMessage
            if (lastMessage != null) {
                binding.lastTime.text = lastMessage.timeString()
                binding.messageText.text =
                    if (showFullMessage) lastMessage.text else lastMessage.shortText()
                if (!lastMessage.isMine && lastMessage.viewed == 0) {
                    binding.newMessageStatus.visibility = View.VISIBLE
                    binding.messageText.setTextColor(ContextCompat.getColor(context, R.color.green))
                    binding.messageText.setTypeface(binding.messageText.typeface, Typeface.BOLD)
                } else {
                    binding.newMessageStatus.visibility = View.GONE
                    binding.messageText.setTextColor(ContextCompat.getColor(context, R.color.black))
                    binding.messageText.setTypeface(binding.messageText.typeface, Typeface.NORMAL)
                }
            }
        }
    }

    override fun onItemSelected(binding: ConversationLayoutBinding, selected: Boolean) {
        val context = binding.root.context
        val color = if (selected)
            ContextCompat.getColor(context, R.color.not_my_message_color)
        else
            ContextCompat.getColor(context, R.color.conversation_background)
        binding.cardView.setCardBackgroundColor(color)
        binding.userPhoto.setForegroundColor(color)
    }
}