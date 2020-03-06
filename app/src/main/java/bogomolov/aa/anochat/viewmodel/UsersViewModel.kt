package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UsersViewModel
@Inject constructor(val repository: Repository) : ViewModel() {
    val usersLiveData = MutableLiveData<List<User>>()

    fun search(startWith: String) {
        viewModelScope.launch(Dispatchers.IO) {
            usersLiveData.postValue(repository.findUsers(startWith))
        }
    }

    fun createConversation(user: User, onCreate: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversationId = repository.getConversation(user)
            withContext(Dispatchers.Main) {
                onCreate(conversationId)
            }
        }
    }
}