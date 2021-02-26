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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val settings: Settings = Settings()
) : UiState

class UpdateStatusAction(val status: String) : UserAction
class UpdatePhotoAction(val photo: String) : UserAction
class UpdateNameAction(val name: String) : UserAction
class LoadSettingsAction : UserAction
class LoadMyUserAction : UserAction
class ChangeSettingsAction(val change: Settings.() -> Settings) : UserAction

class SettingsViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val authRepository: AuthRepository
) : BaseViewModel<SettingsUiState>() {
    lateinit var miniature: BitmapWithName

    override fun createInitialState() = SettingsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is UpdateStatusAction) action.execute()
        if (action is UpdatePhotoAction) action.execute()
        if (action is UpdateNameAction) action.execute()
        if (action is LoadSettingsAction) action.execute()
        if (action is LoadMyUserAction) action.execute()
        if (action is ChangeSettingsAction) action.execute()
    }

    private fun UpdateStatusAction.execute() {
        val user = state.user
        if (user != null) updateMyUser(user.copy(status = status))
    }

    private fun UpdatePhotoAction.execute() {
        val user = state.user
        if (user != null) updateMyUser(user.copy(photo = photo))
    }

    private fun UpdateNameAction.execute() {
        val user = state.user
        if (user != null) updateMyUser(user.copy(name = name))
    }

    private fun updateMyUser(user: User) {
        viewModelScope.launch(dispatcher) {
            setState { copy(user = user) }
            userUseCases.updateMyUser(user)
        }
    }

    private suspend fun LoadSettingsAction.execute() {
        val settings = authRepository.getSettings()
        setState { copy(settings = settings) }
    }

    private suspend fun LoadMyUserAction.execute() {
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
