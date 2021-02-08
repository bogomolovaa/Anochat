package bogomolov.aa.anochat.features.conversations.dialog.actions

import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.features.conversations.dialog.ConversationViewModel
import bogomolov.aa.anochat.features.shared.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SendMessageAction(
    private val text: String,
    private val replyId: String?,
    private val audio: String?
) : UserAction<ConversationViewModel> {

    override suspend fun execute(viewModel: ConversationViewModel) {
        val conversation = viewModel.currentState.conversation
        val repository = viewModel.repository
        if (conversation != null) {
            val message = Message(
                text = text,
                time = System.currentTimeMillis(),
                conversationId = conversation.id,
                replyMessage = if (replyId != null) Message(messageId = replyId) else null,
                audio = audio
            )
            if (audio == null) {
                repository.saveMessage(message, conversation.id)
                repository.sendMessage(message)
            } else {
                repository.saveMessage(message, conversation.id)
                if (repository.uploadFile(audio, conversation.user.uid, true)) {
                    repository.sendMessage(message)
                }
            }
        }
    }
}