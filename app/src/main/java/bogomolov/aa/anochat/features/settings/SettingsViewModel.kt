package bogomolov.aa.anochat.features.settings

import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.BitmapWithName
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val settings: Settings = Settings()
) : UiState

class UpdateUserAction(val change: User.() -> User) : UserAction
class InitSettingsAction : UserAction
class ChangeSettingsAction(val change: Settings.() -> Settings) : UserAction

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val authRepository: AuthRepository
) : BaseViewModel<SettingsUiState>() {
    lateinit var miniature: BitmapWithName

    init {
        addAction(InitSettingsAction())
    }

    override fun createInitialState() = SettingsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is UpdateUserAction) action.execute()
        if (action is InitSettingsAction) action.execute()
        if (action is ChangeSettingsAction) action.execute()
    }

    private fun UpdateUserAction.execute() {
        val user = state.user?.change()
        if (user != null)
            viewModelScope.launch(dispatcher) {
                setState { copy(user = user) }
                userUseCases.updateMyUser(user)
            }
    }

    private suspend fun InitSettingsAction.execute() {
        val settings = authRepository.getSettings()
        setState { copy(settings = settings) }
        viewModelScope.launch(dispatcher) {
            val user = userUseCases.getMyUser()
            setState { copy(user = user) }
        }
    }

    private suspend fun ChangeSettingsAction.execute() {
        val settings = state.settings.change()
        setState { copy(settings = settings) }
        authRepository.updateSettings(settings)
    }
}
