package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.DefaultUserAction

class LoadSettingsAction : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val settings = context.repository.getSettings()
        context.viewModel.setState { copy(settings = settings) }
    }
}