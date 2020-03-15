package bogomolov.aa.anochat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.repository.Repository
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginState { LOGGED, CODE_SENT, NOT_LOGGED }

class SignInViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val loginStateLiveData = MutableLiveData<LoginState>()
    var verificationId: String? = null
    var phoneNumber: String? = null

    fun signIn(credential: PhoneAuthCredential) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = if (repository.signIn(
                    phoneNumber!!,
                    credential
                )
            ) LoginState.LOGGED else LoginState.NOT_LOGGED
            loginStateLiveData.postValue(state)
        }
    }

}