package bogomolov.aa.anochat.features.settings

import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.BitmapWithName
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val settings: Settings = Settings()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val authRepository: AuthRepository
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {
    lateinit var miniature: BitmapWithName

    init {
        initSettings()
    }

    private fun initSettings() = execute {
        val settings = authRepository.getSettings()
        setState { copy(settings = settings) }
        val user = userUseCases.getMyUser()
        setState { copy(user = user) }
    }

    fun changeSettings(change: Settings.() -> Settings) = execute {
        val settings = currentState.settings.change()
        setState { copy(settings = settings) }
        authRepository.updateSettings(settings)
    }

    fun updateUser(change: User.() -> User) = execute {
        val user = currentState.user?.change()
        if (user != null) {
            setState { copy(user = user) }
            userUseCases.updateMyUser(user)
        }
    }
}
