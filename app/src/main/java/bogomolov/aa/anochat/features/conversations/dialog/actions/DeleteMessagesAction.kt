package bogomolov.aa.anochat.features.conversations.dialog.actions

import bogomolov.aa.anochat.features.conversations.dialog.DialogUiState
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.DefaultUserAction

class DeleteMessagesAction(val ids: Set<Long>) : DefaultUserAction<DialogUiState>() {

    override suspend fun execute(context: DefaultContext<DialogUiState>) {
        context.repository.deleteMessages(ids)
    }
}