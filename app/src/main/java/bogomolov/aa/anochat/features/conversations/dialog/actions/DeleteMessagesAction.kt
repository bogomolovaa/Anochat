package bogomolov.aa.anochat.features.conversations.dialog.actions

import bogomolov.aa.anochat.features.conversations.dialog.ConversationActionContext
import bogomolov.aa.anochat.features.shared.mvi.UserAction

class DeleteMessagesAction(val ids: Set<Long>) : UserAction<ConversationActionContext> {

    override suspend fun execute(context: ConversationActionContext) {
        context.repository.deleteMessages(ids)
    }
}