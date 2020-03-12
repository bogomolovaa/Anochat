package bogomolov.aa.anochat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginState { LOGGED, NOT_LOGGED }

class SignInViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val loginStateLiveData = MutableLiveData<LoginState>()

    fun getSavedEmail(context: Context): String{
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString("email","")!!
    }

    fun signIn(email: String, password: String) {
        Log.i("test","signIn $email $password")
        viewModelScope.launch(Dispatchers.IO) {
            val state = if(repository.signIn(email, password)) LoginState.LOGGED else LoginState.NOT_LOGGED
            loginStateLiveData.postValue(state)
        }
    }

}