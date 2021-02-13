package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.DefaultUserAction

class LoadMyUserAction : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.repository.getMyUser()
        context.viewModel.setState { copy(user = user) }
    }
}