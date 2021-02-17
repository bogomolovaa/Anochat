package bogomolov.aa.anochat.features.login

import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.features.shared.AuthRepository
import com.google.firebase.auth.PhoneAuthCredential
import javax.inject.Inject

enum class LoginState { LOGGED, CODE_SENT, NOT_LOGGED }

data class SignInUiState(
    val state: LoginState,
    val verificationId: String? = null,
    val phoneNumber: String? = null
) : UiState

class SignInAction(val credential: PhoneAuthCredential) : UserAction

class SignInViewModel
@Inject constructor(private val authRepository: AuthRepository) : BaseViewModel<SignInUiState>() {

    override fun createInitialState() = SignInUiState(LoginState.NOT_LOGGED)

    override suspend fun handleAction(action: UserAction) {
        if (action is SignInAction) action.execute()
    }

    private suspend fun SignInAction.execute() {
        val phoneNumber = state.phoneNumber!!
        val succeed = authRepository.signIn(phoneNumber, credential)
        val state = if (succeed) LoginState.LOGGED else LoginState.NOT_LOGGED
        setState { copy(state = state) }
    }
}