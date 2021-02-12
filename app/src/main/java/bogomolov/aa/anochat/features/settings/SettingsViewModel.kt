package bogomolov.aa.anochat.features.settings

import bogomolov.aa.anochat.domain.Settings
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.mvi.RepositoryBaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val settings: Settings = Settings()
) : UiState

class SettingsViewModel @Inject constructor(repository: Repository) :
    RepositoryBaseViewModel<SettingsUiState>(repository) {

    override fun createInitialState() = SettingsUiState()
}