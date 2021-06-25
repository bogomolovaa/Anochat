package bogomolov.aa.anochat.features.login

import android.app.Activity
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.entity.isValidPhone
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.ErrorType
import bogomolov.aa.anochat.features.shared.PhoneVerification
import bogomolov.aa.anochat.features.shared.SignInError
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginState {
    INITIAL,
    PHONE_SUBMITTED,
    VERIFICATION_ID_RECEIVED,
    CODE_SUBMITTED,
    NOT_LOGGED,
    LOGGED
}

data class SignInUiState(
    val state: LoginState = LoginState.INITIAL,
    val phoneNumber: String? = null,
    val code: String? = null,
    val error: SignInError? = null
)

object NavigateToConversationList : Event

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<SignInUiState>(SignInUiState()) {

    val phoneVerification = object : PhoneVerification {

        override fun onComplete() {
            updateState { copy(state = LoginState.LOGGED) }
            viewModelScope.launch { addEvent(NavigateToConversationList) }
        }

        override fun onCodeVerify(smsCode: String?) {
            updateState { copy(state = LoginState.CODE_SUBMITTED, code = smsCode) }
        }

        override fun onCodeSent() {
            updateState { copy(state = LoginState.VERIFICATION_ID_RECEIVED, error = null) }
        }

        override fun onPhoneError(error: SignInError?) {
            updateState { copy(state = LoginState.INITIAL, error = error) }
        }

        override fun onCodeError(error: SignInError?) {
            updateState { copy(state = LoginState.NOT_LOGGED, error = error) }
        }
    }

    fun submitPhoneNumber(getActivity: () -> Activity) = execute {
        val number = currentState.phoneNumber ?: ""
        if (number.isNotEmpty() && isValidPhone(number)) {
            setState { copy(phoneNumber = number, state = LoginState.PHONE_SUBMITTED) }
            authRepository.sendPhoneNumber(number, getActivity, phoneVerification)
        } else {
            setState { copy(phoneNumber = number, error = SignInError(ErrorType.WRONG_PHONE)) }
        }
    }

    fun submitSmsCode() = execute {
        val code = currentState.code ?: ""
        if (code.isNotEmpty()) {
            authRepository.verifySmsCode(currentState.phoneNumber!!, code, phoneVerification)
        } else {
            setState { copy(error = SignInError(ErrorType.EMPTY_CODE)) }
        }
    }
}

val testSignInUiState = SignInUiState(
    state = LoginState.CODE_SUBMITTED,
    phoneNumber = "+71234567",
    code = "123456",
    error = SignInError(ErrorType.WRONG_CODE)
)