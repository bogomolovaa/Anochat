package bogomolov.aa.anochat.features.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val settings: Settings = Settings(),
    val miniatureState: MiniatureState? = null
)

data class MiniatureState(
    val miniature: BitmapWithName,
    val initialImageScale: Float = 1f,
    val maskX: Int = 0,
    val maskY: Int = 0,
    val scaling: Boolean = false,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val maxScale: Float = 1f,
    val lastPoint: Pair<Int, Int> = Pair(0, 0),
    val canMove: Boolean = true,
    val maskImage: MaskImage = MaskImage()
)

data class MaskImage(
    val scaleFactor: Float = 1f,
    val left: Int = 0,
    val top: Int = 0,
    val width: Int  = 100,
    val height: Int = 100
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val authRepository: AuthRepository
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    init {
        initSettings()
    }

    fun setMiniature(miniature: BitmapWithName) = execute {
        setState { copy(miniatureState = MiniatureState(miniature)) }
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

val testSettingsUiState = SettingsUiState(
    user = User(phone = "+12345671", name = "name1", status = "status1"),
    settings = Settings()
)
