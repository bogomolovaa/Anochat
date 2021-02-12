package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.domain.Settings
import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.DefaultUserAction

class ChangeSettingsAction(private val change: Settings.() -> Settings) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val settings = context.viewModel.currentState.settings.change()
        context.viewModel.setState { copy(settings = settings) }
        context.repository.updateSettings(settings)
    }
}