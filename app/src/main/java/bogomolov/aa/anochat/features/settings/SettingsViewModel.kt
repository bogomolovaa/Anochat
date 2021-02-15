package bogomolov.aa.anochat.features.settings

import bogomolov.aa.anochat.domain.entity.Settings
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.repository.repositories.UserRepository
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

class SettingsViewModel @Inject constructor(private val userRepository: UserRepository) :
    BaseViewModel<SettingsUiState>() {

    override fun createInitialState() = SettingsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is UpdateStatusAction) action.execute()
        if (action is UpdatePhotoAction) action.execute()
        if (action is UpdateNameAction) action.execute()
        if (action is LoadSettingsAction) action.execute()
        if (action is LoadMyUserAction) action.execute()
        if (action is ChangeSettingsAction) action.execute()
    }

    private suspend fun UpdateStatusAction.execute() {
        val user = currentState.user!!
        val changedUser = user.copy(status = status)
        setState { copy(user = changedUser) }
        userRepository.updateMyUser(changedUser)
    }

    private suspend fun UpdatePhotoAction.execute() {
        val user = currentState.user!!
        val changedUser = user.copy(photo = photo)
        setState { copy(user = changedUser) }
        userRepository.updateMyUser(changedUser)
    }

    private suspend fun UpdateNameAction.execute() {
        val user = currentState.user!!
        val changedUser = user.copy(name = name)
        setState { copy(user = changedUser) }
        userRepository.updateMyUser(changedUser)
    }

    private suspend fun LoadSettingsAction.execute() {
        val settings = userRepository.getSettings()
        setState { copy(settings = settings) }
    }

    private suspend fun LoadMyUserAction.execute() {
        val user = userRepository.getMyUser()
        setState { copy(user = user) }
    }

    private suspend fun ChangeSettingsAction.execute() {
        val settings = currentState.settings.change()
        setState { copy(settings = settings) }
        userRepository.updateSettings(settings)
    }
}
