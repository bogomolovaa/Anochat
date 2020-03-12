package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val userLiveData = MutableLiveData<User>()

    fun loadUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userLiveData.postValue(repository.findUser(uid))
        }
    }

    fun updatePhoto(photo: String) {
        val user = userLiveData.value
        if (user != null)
            viewModelScope.launch(Dispatchers.IO) {
                user.photo = photo
                repository.updateUser(user)
            }
    }
}