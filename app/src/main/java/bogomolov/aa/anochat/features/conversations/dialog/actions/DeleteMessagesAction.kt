package bogomolov.aa.anochat.features.conversations.dialog.actions

import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.features.conversations.dialog.ConversationActionContext
import bogomolov.aa.anochat.features.conversations.dialog.ConversationViewModel
import bogomolov.aa.anochat.features.shared.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteMessagesAction(val ids: Set<Long>) : UserAction<ConversationActionContext> {

    override suspend fun execute(context: ConversationActionContext) {
        val saveIds = HashSet(ids)
        context.repository.deleteMessages(saveIds)
    }
}