package bogomolov.aa.anochat.features.conversations.dialog.actions

import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.features.conversations.dialog.ConversationViewModel
import bogomolov.aa.anochat.features.shared.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteMessagesAction(val ids: Set<Long>) : UserAction<ConversationViewModel> {

    override suspend fun execute(viewModel: ConversationViewModel) {
        val saveIds = HashSet(ids)
        viewModel.repository.deleteMessages(saveIds)
    }
}