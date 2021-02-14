package bogomolov.aa.anochat.features.login

import bogomolov.aa.anochat.features.shared.mvi.*
import bogomolov.aa.anochat.repository.Repository
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
@Inject constructor(private val repository: Repository) : BaseViewModel<SignInUiState>() {

    override fun createInitialState() = SignInUiState(LoginState.NOT_LOGGED)

    override suspend fun handleAction(action: UserAction) {
        if (action is SignInAction) action.execute()
    }

    private suspend fun SignInAction.execute() {
        val phoneNumber = currentState.phoneNumber!!
        val succeed = repository.signIn(phoneNumber, credential)
        val state = if (succeed) LoginState.LOGGED else LoginState.NOT_LOGGED
        setState { copy(state = state) }
    }
}