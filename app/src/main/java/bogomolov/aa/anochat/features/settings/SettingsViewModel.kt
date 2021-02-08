package bogomolov.aa.anochat.features.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.BaseViewModel
import bogomolov.aa.anochat.features.shared.UiState
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.Repository
import bogomolov.aa.anochat.repository.getFilePath
import bogomolov.aa.anochat.repository.getMiniPhotoFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.lang.Math.max
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val status: String? = null
) : UiState

class SettingsViewModel
@Inject constructor(val repository: Repository) :
    BaseViewModel<SettingsUiState, SettingsViewModel>() {

    override fun createInitialState() = SettingsUiState()
}

class UpdateNameAction(val name: String) : UserAction<SettingsViewModel> {

    override suspend fun execute(viewModel: SettingsViewModel) {
        val user = viewModel.currentState.user
        if (user != null) {
            user.name = name
            viewModel.repository.updateUserTo(user)
            viewModel.setState { copy(user = user) }
        }
    }
}

class UpdateStatusAction(val status: String) : UserAction<SettingsViewModel> {

    override suspend fun execute(viewModel: SettingsViewModel) {
        val user = viewModel.currentState.user
        Log.i("UpdateStatusAction","execute status $status")
        if (user != null) {
            user.status = status
            viewModel.repository.updateUserTo(user)
            Log.i("UpdateStatusAction","execute setState $user")
            viewModel.setState { copy(status = status) }
        }
    }
}

class LoadUserAction(val uid: String) : UserAction<SettingsViewModel> {

    override suspend fun execute(viewModel: SettingsViewModel) {
        val user = viewModel.repository.getUser(uid, true)
        viewModel.setState { copy(user = user) }
    }
}

class UpdatePhotoAction(
    val photo: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) : UserAction<SettingsViewModel> {

    override suspend fun execute(viewModel: SettingsViewModel) {
        val user = viewModel.currentState.user
        if (user != null) {
            val filePath = getFilePath(viewModel.repository.getContext(), photo)
            val bitmap = BitmapFactory.decodeFile(filePath)
            val miniBitmap = Bitmap.createBitmap(
                bitmap,
                max(x, 0),
                max(y, 0),
                Math.min(bitmap.width - max(x, 0), width),
                Math.min(bitmap.height - max(y, 0), height)
            )
            val miniPhotoPath = getFilePath(
                viewModel.repository.getContext(),
                getMiniPhotoFileName(viewModel.repository.getContext(), photo)
            )
            miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))
            user.photo = photo
            viewModel.setState { copy(user = user) }
        }
    }
}