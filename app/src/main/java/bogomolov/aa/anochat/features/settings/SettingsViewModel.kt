package bogomolov.aa.anochat.features.settings

import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.repository.*
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val notifications: Boolean = false,
    val sound: Boolean = false,
    val vibration: Boolean = false
) : UiState

class SettingsViewModel @Inject constructor(repository: Repository) :
    RepositoryBaseViewModel<SettingsUiState>(repository) {

    override fun createInitialState() = SettingsUiState()
}