package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.DefaultContext
import bogomolov.aa.anochat.features.shared.DefaultUserAction

class UpdateStatusAction(val status: String) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.viewModel.currentState.user
        if (user != null) {
            context.repository.updateUserTo(user)
            context.viewModel.setState { copy(user = user.copy(status = status)) }
        }
    }
}