package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.DefaultUserAction

class UpdateNameAction(val name: String) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.viewModel.currentState.user!!
        val changedUser = user.copy(name = name)
        context.repository.updateMyUser(changedUser)
        context.viewModel.setState { copy(user = changedUser) }
    }
}