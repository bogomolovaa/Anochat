package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.DefaultContext
import bogomolov.aa.anochat.features.shared.DefaultUserAction
import bogomolov.aa.anochat.repository.Setting
import bogomolov.aa.anochat.repository.setSetting

class UpdateSettingAction(private val setting: String, private val value: Boolean) :
    DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        context.repository.setSetting(setting, value)
        context.viewModel.setState {
            when (setting) {
                Setting.NOTIFICATIONS -> copy(notifications = value)
                Setting.SOUND -> copy(sound = value)
                Setting.VIBRATION -> copy(vibration = value)
                else -> this
            }
        }
    }
}