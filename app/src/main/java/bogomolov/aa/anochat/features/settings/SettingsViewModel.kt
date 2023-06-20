package bogomolov.aa.anochat.features.settings

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.BitmapWithName
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.Event
import bogomolov.aa.anochat.repository.FileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

enum class SettingEditType { EDIT_USERNAME, EDIT_STATUS }

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
    val width: Int = 100,
    val height: Int = 100
)

object MiniatureCreatedEvent : Event
object PhotoResizedEvent : Event

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val authRepository: AuthRepository,
    private val fileStore: FileStore
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    init {
        initSettings()
    }

    fun resizePhoto(uri: Uri) {
        viewModelScope.launch(dispatcher) {
            fileStore.resizeImage(uri = uri, toGallery = false)?.let {
                updateState { copy(miniatureState = MiniatureState(it)) }
                addEvent(PhotoResizedEvent)
            }
        }
    }

    fun createMiniature(miniPhotoPath: String) {
        viewModelScope.launch(dispatcher) {
            currentState.miniatureState?.let { state ->
                val maskWidth = state.maskImage.width * state.maskImage.scaleFactor
                val maskHeight = state.maskImage.height * state.maskImage.scaleFactor
                val x = (state.maskX / state.initialImageScale).toInt()
                val y = (state.maskY / state.initialImageScale).toInt()
                val width = (maskWidth / state.initialImageScale).toInt()
                val height = (maskHeight / state.initialImageScale).toInt()

                val miniature = state.miniature
                val bitmap = miniature.bitmap!!
                val miniBitmap = Bitmap.createBitmap(
                    bitmap,
                    max(x, 0),
                    max(y, 0),
                    min(bitmap.width - Math.max(x, 0), width),
                    min(bitmap.height - Math.max(y, 0), height)
                )
                miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))
                updateUser { copy(photo = state.miniature.name) }
            }
            addEvent(MiniatureCreatedEvent)
        }
    }

    private fun initSettings() {
        viewModelScope.launch {
            val settings = authRepository.getSettings()
            updateState { copy(settings = settings) }
            val user = userUseCases.getMyUser()
            updateState { copy(user = user) }
        }
    }

    fun updateSettings(change: Settings.() -> Settings) {
        viewModelScope.launch {
            val settings = currentState.settings.change()
            updateState { copy(settings = settings) }
            authRepository.updateSettings(settings)
        }
    }

    fun updateUser(change: User.() -> User) {
        viewModelScope.launch {
            currentState.user?.change()?.let {
                updateState { copy(user = it) }
                userUseCases.updateMyUser(it)
            }
        }
    }

    fun initDimensions(
        measuredWidth: Int,
        measuredHeight: Int,
        windowWidth: Int
    ) {
        if (currentState.miniatureState?.imageWidth == 0)
            updateState {
                copy(
                    miniatureState = miniatureState?.initMiniatureState(measuredWidth, measuredHeight)
                        ?.checkBounds(scale = windowWidth.toFloat() / miniatureState.maskImage.width)
                )
            }
    }

    private fun MiniatureState.initMiniatureState(
        measuredWidth: Int,
        measuredHeight: Int,
    ): MiniatureState {
        val maskImageWidth = maskImage.width
        val maskImageHeight = maskImage.height
        val bitmap = miniature.bitmap!!
        val koef1 = bitmap.width.toFloat() / bitmap.height.toFloat()
        val koef2 = measuredWidth.toFloat() / measuredHeight.toFloat()
        val imageWidth: Int
        val imageHeight: Int
        val initialImageScale: Float
        if (koef1 >= koef2) {
            imageWidth = measuredWidth
            imageHeight = (measuredWidth / koef1).toInt()
            initialImageScale = measuredWidth.toFloat() / bitmap.width.toFloat()
        } else {
            imageWidth = (koef1 * measuredHeight).toInt()
            imageHeight = measuredHeight
            initialImageScale = measuredHeight.toFloat() / bitmap.height.toFloat()
        }
        val maxScaleX = imageWidth.toFloat() / maskImageWidth
        val maxScaleY = imageHeight.toFloat() / maskImageHeight
        val maxScale = min(maxScaleX, maxScaleY)
        return copy(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            initialImageScale = initialImageScale,
            maxScale = maxScale,
        )
    }

    fun checkBounds(offset: Boolean, x: Int, y: Int, zoom: Float) {
        updateState {
            copy(
                miniatureState = miniatureState?.checkBounds(
                    offset = if (offset) Pair(x, y) else null,
                    scale = miniatureState.maskImage.scaleFactor * zoom,
                )
            )
        }
    }

    private fun MiniatureState.checkBounds(
        offset: Pair<Int, Int>? = null,
        scale: Float? = null
    ): MiniatureState {
        val scaleFactor = scale ?: maskImage.scaleFactor
        var left = maskImage.left
        var top = maskImage.top
        offset?.let {
            left += it.first
            top += it.second
        }
        val maskImageWidth = maskImage.width * scaleFactor
        val maskImageHeight = maskImage.height * scaleFactor
        top = top.coerceAtLeast(0)
        left = left.coerceAtLeast(0)
        top = top.coerceAtMost((imageHeight - maskImageHeight).toInt())
        left = left.coerceAtMost((imageWidth - maskImageWidth).toInt())
        val maskX = left
        val maskY = top
        return copy(
            maskX = maskX,
            maskY = maskY,
            maskImage = maskImage.copy(
                scaleFactor = max(0.5f, min(scaleFactor, maxScale)),
                left = left,
                top = top
            )
        )
    }
}

val testSettingsUiState = SettingsUiState(
    user = User(phone = "+12345671", name = "name1", status = "status1"),
    settings = Settings()
)
