package bogomolov.aa.anochat.features.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.repository.*
import java.io.FileOutputStream
import java.lang.Math.max
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


class UpdateNameAction(val name: String) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.viewModel.currentState.user
        if (user != null) {
            user.name = name
            context.repository.updateUserTo(user)
            context.viewModel.setState { copy(user = user) }
        }
    }
}

class UpdateStatusAction(val status: String) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.viewModel.currentState.user
        if (user != null) {
            context.repository.updateUserTo(user)
            context.viewModel.setState { copy(user = user.copy(status = status)) }
        }
    }
}

class LoadSettingsAction() : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val notifications: Boolean = context.repository.getSetting(Setting.NOTIFICATIONS)!!
        val sound: Boolean = context.repository.getSetting(Setting.SOUND)!!
        val vibration: Boolean = context.repository.getSetting(Setting.VIBRATION)!!
        context.viewModel.setState {
            copy(notifications = notifications, sound = sound, vibration = vibration)
        }
    }
}

class UpdateSettingAction(private val setting: Setting, private val value: Boolean) :
    DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        context.repository.setSetting(setting, value)
        context.viewModel.setState {
            when (setting) {
                Setting.NOTIFICATIONS -> copy(notifications = value)
                Setting.SOUND -> copy(sound = value)
                Setting.VIBRATION -> copy(vibration = value)
            }
        }
    }
}

class LoadUserAction(val uid: String) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.repository.getUser(uid, true)
        context.viewModel.setState { copy(user = user) }
    }
}

class UpdatePhotoAction(
    val photo: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.viewModel.currentState.user
        if (user != null) {
            val filePath = getFilePath(context.repository.getContext(), photo)
            val bitmap = BitmapFactory.decodeFile(filePath)
            val miniBitmap = Bitmap.createBitmap(
                bitmap,
                max(x, 0),
                max(y, 0),
                Math.min(bitmap.width - max(x, 0), width),
                Math.min(bitmap.height - max(y, 0), height)
            )
            val userWithNewPhoto = user.copy(photo = photo)
            context.viewModel.setState { copy(user = userWithNewPhoto) }
            context.repository.updateUserTo(userWithNewPhoto)
            val appContext = context.repository.getContext()
            val miniPhotoPath = getFilePath(appContext, getMiniPhotoFileName(appContext, photo))
            miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))
        }
    }
}