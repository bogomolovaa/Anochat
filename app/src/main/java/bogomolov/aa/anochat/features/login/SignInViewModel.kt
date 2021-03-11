package bogomolov.aa.anochat.features.login

import android.app.Activity
import bogomolov.aa.anochat.domain.entity.isValidPhone
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.ErrorType
import bogomolov.aa.anochat.features.shared.PhoneVerification
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: ErrorType? = null
) : UiState

class SubmitPhoneNumberAction(val number: String, val activity: () -> Activity) : UserAction
class SubmitSmsCodeAction(val code: String) : UserAction

@HiltViewModel
class SignInViewModel
@Inject constructor(private val authRepository: AuthRepository) : BaseViewModel<SignInUiState>() {

    override fun createInitialState() = SignInUiState()

    private val phoneVerification = object : PhoneVerification {

        override fun onComplete() {
            setStateAsync { copy(state = LoginState.LOGGED) }
        }

        override fun onCodeVerify(smsCode: String?) {
            setStateAsync { copy(state = LoginState.CODE_SUBMITTED, code = smsCode) }
        }

        override fun onCodeSent() {
            setStateAsync { copy(state = LoginState.VERIFICATION_ID_RECEIVED, error = null) }
        }

        override fun onPhoneError(error: ErrorType?) {
            setStateAsync { copy(state = LoginState.INITIAL, error = error) }
        }

        override fun onCodeError(error: ErrorType?) {
            setStateAsync { copy(state = LoginState.NOT_LOGGED, error = error) }
        }
    }

    override suspend fun handleAction(action: UserAction) {
        if (action is SubmitPhoneNumberAction) action.execute()
        if (action is SubmitSmsCodeAction) action.execute()
    }

    private suspend fun SubmitPhoneNumberAction.execute() {
        if (number.isNotEmpty() && isValidPhone(number)) {
            setState { copy(phoneNumber = number, state = LoginState.PHONE_SUBMITTED) }
            authRepository.sendPhoneNumber(number, activity, phoneVerification)
        } else {
            setState { copy(phoneNumber = number, error = ErrorType.WRONG_PHONE) }
        }
    }

    private suspend fun SubmitSmsCodeAction.execute() {
        if (code.isNotEmpty()) {
            authRepository.verifySmsCode(state.phoneNumber!!, code, phoneVerification)
        } else {
            setState { copy(error = ErrorType.EMPTY_CODE) }
        }
    }
}