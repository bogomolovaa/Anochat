package bogomolov.aa.anochat.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RegisterState { REGISTERED, NOT_REGISTERED }

class SignUpViewModel
@Inject constructor(val repository: Repository): ViewModel(){
    val registerStateLiveData = MutableLiveData<RegisterState>()

    fun signUp(name: String, email: String, password: String) {
        Log.i("test","signOut $name $email $password")
        viewModelScope.launch(Dispatchers.IO) {
            val state = if(repository.signUp(name, email, password)) RegisterState.REGISTERED else RegisterState.NOT_REGISTERED
            registerStateLiveData.postValue(state)
        }
    }
}