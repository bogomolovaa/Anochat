package bogomolov.aa.anochat.features.conversations.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.ConversationLayoutBinding
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.ActionModeData
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter

class ConversationsPagedAdapter(
    private val showFullMessage: Boolean = false,
    actionModeData: ActionModeData<Conversation>? = null,
) : ExtPagedListAdapter<Conversation, ConversationLayoutBinding>(actionModeData) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ConversationLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, cv, binding)
    }

    override fun getId(item: Conversation) = item.id

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(conversation: Conversation?, binding: ConversationLayoutBinding) {
        if (conversation != null) {
            binding.conversation = conversation
            binding.messageText.text =
                if (showFullMessage) {
                    conversation.lastMessage?.text ?: ""
                } else {
                    conversation.lastMessage?.shortText() ?: ""
                }
            setClickListener(conversation, binding)
        }
    }

    private fun setClickListener(conversation: Conversation, binding: ConversationLayoutBinding) {
        binding.cardView.setOnClickListener {
            val navController = binding.cardView.findNavController()
            val bundle = Bundle().apply { putLong("id", conversation.id) }
            navController.navigate(R.id.dialog_graph, bundle)
        }
    }
}