package bogomolov.aa.anochat.features.login

import bogomolov.aa.anochat.features.shared.BaseViewModel
import bogomolov.aa.anochat.features.shared.UiState
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.Repository
import com.google.firebase.auth.PhoneAuthCredential
import javax.inject.Inject

enum class LoginState { LOGGED, CODE_SENT, NOT_LOGGED }

data class SignInUiState(
    val state: LoginState,
    val verificationId: String? = null,
    val phoneNumber: String? = null
) : UiState

class SignInViewModel
@Inject constructor(val repository: Repository) : BaseViewModel<SignInUiState, SignInViewModel>() {

    override fun createInitialState() = SignInUiState(LoginState.NOT_LOGGED)
}

class SignInAction(private val credential: PhoneAuthCredential) : UserAction<SignInViewModel> {

    override suspend fun execute(viewModel: SignInViewModel) {
        val phoneNumber = viewModel.currentState.phoneNumber!!
        val succeed = viewModel.repository.signIn(phoneNumber, credential)
        val state = if (succeed) LoginState.LOGGED else LoginState.NOT_LOGGED
        viewModel.setState { copy(state = state) }
    }
}

