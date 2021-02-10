package bogomolov.aa.anochat.features.login

import bogomolov.aa.anochat.features.shared.*
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
@Inject constructor(repository: Repository) :
    RepositoryBaseViewModel<SignInUiState>(repository) {

    override fun createInitialState() = SignInUiState(LoginState.NOT_LOGGED)
}

class SignInAction(private val credential: PhoneAuthCredential) : UserAction<DefaultContext<SignInUiState>> {

    override suspend fun execute(context: DefaultContext<SignInUiState>) {
        val phoneNumber = context.viewModel.currentState.phoneNumber!!
        val succeed = context.repository.signIn(phoneNumber, credential)
        val state = if (succeed) LoginState.LOGGED else LoginState.NOT_LOGGED
        context.viewModel.setState { copy(state = state) }
    }
}

