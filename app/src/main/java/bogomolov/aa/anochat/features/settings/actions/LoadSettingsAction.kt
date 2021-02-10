package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.DefaultContext
import bogomolov.aa.anochat.features.shared.DefaultUserAction
import bogomolov.aa.anochat.repository.Setting
import bogomolov.aa.anochat.repository.getSetting

class LoadSettingsAction() : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val notifications: Boolean = context.repository.getSetting(Setting.NOTIFICATIONS)!!
        val sound: Boolean = context.repository.getSetting(Setting.SOUND)!!
        val vibration: Boolean = context.repository.getSetting(Setting.VIBRATION)!!
        context.viewModel.setState {
            copy(notifications = notifications, sound = sound, vibration = vibration)
        }
    }
}