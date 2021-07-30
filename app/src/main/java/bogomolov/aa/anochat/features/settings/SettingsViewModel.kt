package bogomolov.aa.anochat.features.settings

import android.net.Uri
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.BitmapWithName
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.Event
import bogomolov.aa.anochat.repository.FileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class SettingEditType { EDIT_USERNAME, EDIT_STATUS }

data class SettingsUiState(
    val user: User? = null,
    val settings: Settings = Settings(),
    val miniatureState: MiniatureState? = null,
    val settingEditType: SettingEditType? = null,
    val settingText: String = ""
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
    val width: Int = 100,
    val height: Int = 100
)

object MiniatureCreatedEvent: Event

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val authRepository: AuthRepository,
    private val fileStore: FileStore
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    init {
        initSettings()
    }

    fun createMiniature(uri: Uri) = execute {
        val miniature = fileStore.resizeImage(uri = uri, toGallery = false)
        if (miniature != null){
            setState { copy(miniatureState = MiniatureState(miniature)) }
            addEvent(MiniatureCreatedEvent)
        }
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
