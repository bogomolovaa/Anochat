package bogomolov.aa.anochat.features.login

import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import javax.inject.Inject

enum class LoginState {
    INITIAL,
    PHONE_SENT,
    VERIFICATION_ID_RECEIVED,
    CODE_SENT,
    NOT_LOGGED,
    LOGGED
}

enum class ErrorType {
    WRONG_CODE,
    EMPTY_CODE,
    WRONG_PHONE,
    PHONE_NO_CONNECTION,
    CODE_NO_CONNECTION
}

data class SignInUiState(
    val state: LoginState = LoginState.INITIAL,
    val verificationId: String? = null,
    val phoneNumber: String? = null,
    val code: String? = null,
    val error: ErrorType? = null
) : UiState

class SignInAction(val credential: PhoneAuthCredential, val smsCode: String?) : UserAction

class SignInViewModel
@Inject constructor(private val authRepository: AuthRepository) : BaseViewModel<SignInUiState>() {

    override fun createInitialState() = SignInUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is SignInAction) action.execute()
    }

    private suspend fun SignInAction.execute() {
        val phoneNumber = state.phoneNumber!!
        setState { copy(state = LoginState.CODE_SENT, code = smsCode) }
        try {
            val succeed = authRepository.signIn(phoneNumber, credential)
            if (succeed) setState { copy(state = LoginState.LOGGED) }
        } catch (e: Exception) {
            val error = when (e) {
                is FirebaseAuthInvalidCredentialsException -> ErrorType.WRONG_CODE
                is FirebaseNetworkException -> ErrorType.CODE_NO_CONNECTION
                else -> null
            }
            setState { copy(state = LoginState.NOT_LOGGED, error = error) }
        }
    }
}