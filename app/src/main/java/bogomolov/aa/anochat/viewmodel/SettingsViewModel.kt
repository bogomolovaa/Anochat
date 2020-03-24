package bogomolov.aa.anochat.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.android.getFilePath
import bogomolov.aa.anochat.android.getMiniPhotoFileName
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.lang.Integer.min
import java.lang.Math.max
import javax.inject.Inject

class SettingsViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val userLiveData = MutableLiveData<User>()

    fun loadUser(uid: String) {
        if (userLiveData.value == null) {
            viewModelScope.launch(Dispatchers.IO) {
                userLiveData.postValue(repository.getUser(uid, true))
            }
        }
    }

    private fun updateUser(user: User) {
        userLiveData.value = user
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUserTo(user)
        }
    }

    fun updatePhoto(photo: String, x: Int, y: Int, width: Int, height: Int) {
        val user = userLiveData.value
        if (user != null) {
            val filePath = getFilePath(repository.getContext(), photo)
            val bitmap = BitmapFactory.decodeFile(filePath)
            val miniBitmap = Bitmap.createBitmap(
                bitmap,
                max(x, 0),
                max(y, 0),
                Math.min(bitmap.width - max(x, 0), width),
                Math.min(bitmap.height - max(y, 0), height)
            )
            val miniPhotoPath = getFilePath(
                repository.getContext(),
                getMiniPhotoFileName(repository.getContext(), photo)
            )
            miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))
            user.photo = photo
            updateUser(user)
        }
    }

    fun updateName(name: String) {
        val user = userLiveData.value
        if (user != null) {
            user.name = name
            updateUser(user)
        }
    }

    fun updateStatus(status: String) {
        val user = userLiveData.value
        if (user != null) {
            user.status = status
            updateUser(user)
        }
    }
}