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
import javax.inject.Inject

class SettingsViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val userLiveData = MutableLiveData<User>()

    fun loadUser(uid: String) {
        Log.i("test", "load user $uid")
        viewModelScope.launch(Dispatchers.IO) {
            userLiveData.postValue(repository.getUser(uid))
        }
    }

    private fun updateUser(user: User) {
        userLiveData.value = user
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUserTo(user)
        }
    }

    fun updatePhoto(photo: String, x: Int, y: Int, width: Int, height: Int) {
        Log.i("test", "save mini x $x y $y width $width height $height")
        val user = userLiveData.value
        if (user != null) {
            val filePath = getFilePath(repository.getContext(), photo)
            val bitmap = BitmapFactory.decodeFile(filePath)
            val miniBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
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
        Log.i("test", "updateName $user name to $name")
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