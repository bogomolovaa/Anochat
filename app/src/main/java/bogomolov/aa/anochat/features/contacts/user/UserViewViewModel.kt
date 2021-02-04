package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class UserViewViewModel
@Inject constructor(val repository: Repository) : ViewModel() {
    val userLiveData = MutableLiveData<User>()

    fun loadUser(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser(id)
            userLiveData.postValue(user)
        }
    }

    fun loadImages(id: Long) = LivePagedListBuilder(repository.getImages(id), 10).build()

}