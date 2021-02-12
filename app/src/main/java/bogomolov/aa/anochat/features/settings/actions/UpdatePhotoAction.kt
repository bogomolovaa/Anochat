package bogomolov.aa.anochat.features.settings.actions

import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.mvi.DefaultContext
import bogomolov.aa.anochat.features.shared.mvi.DefaultUserAction

class UpdatePhotoAction(private val photo: String) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.viewModel.currentState.user!!
        val changedUser = user.copy(photo = photo)
        context.viewModel.setState { copy(user = changedUser) }
        context.repository.updateMyUser(changedUser)
    }
}