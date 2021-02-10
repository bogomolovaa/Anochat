package bogomolov.aa.anochat.features.conversations.dialog.actions

import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.features.conversations.dialog.ConversationActionContext
import bogomolov.aa.anochat.features.conversations.dialog.ConversationViewModel
import bogomolov.aa.anochat.features.shared.UserAction

class SendMessageAction(
    private val text: String? = null,
    private val replyId: String? = null,
    private val audio: String? = null,
    private val image: String? = null
) : UserAction<ConversationActionContext> {

    override suspend fun execute(context: ConversationActionContext) {
        val conversation = context.viewModel.currentState.conversation
        val repository = context.repository
        if (conversation != null) {
            val message = Message(
                text = text ?: "",
                time = System.currentTimeMillis(),
                conversationId = conversation.id,
                replyMessage = if (replyId != null) Message(messageId = replyId) else null,
                audio = audio,
                image = image
            )
            repository.saveMessage(message, conversation.id)
            val file = audio ?: image
            if (file == null || repository.uploadFile(file, conversation.user.uid, true)) {
                repository.sendMessage(message)
            }
        }
    }
}