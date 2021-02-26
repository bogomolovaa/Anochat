package bogomolov.aa.anochat.features.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentSignInBinding
import bogomolov.aa.anochat.domain.entity.isValidPhone
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SignInFragment : Fragment(), UpdatableView<SignInUiState> {
    private val viewModel: SignInViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var binding: FragmentSignInBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        binding.toolbar.navigationIcon = null
        binding.fab.setOnClickListener {
            when (viewModel.state.state) {
                LoginState.INITIAL -> submitPhoneNumber()
                LoginState.VERIFICATION_ID_RECEIVED, LoginState.NOT_LOGGED -> submitCode()
                else -> {
                }
            }
        }
        return binding.root
    }

    override fun updateView(newState: SignInUiState, currentState: SignInUiState) {
        binding.phoneInputText.setText(newState.phoneNumber)
        setError(newState.error)
        when (newState.state) {
            LoginState.INITIAL -> {
                binding.progressBar.visibility = View.INVISIBLE
                binding.codeInputLayout.visibility = View.INVISIBLE
                binding.fab.isEnabled = true
            }
            LoginState.PHONE_SENT -> {
                binding.codeInputLayout.visibility = View.INVISIBLE
                binding.fab.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
            }
            LoginState.VERIFICATION_ID_RECEIVED -> {
                binding.progressBar.visibility = View.INVISIBLE
                binding.codeInputLayout.visibility = View.VISIBLE
                binding.codeInputText.setText(newState.code)
                binding.fab.isEnabled = true
            }
            LoginState.CODE_SENT -> {
                binding.codeInputLayout.visibility = View.VISIBLE
                binding.codeInputText.setText(newState.code)
                binding.fab.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
            }
            LoginState.LOGGED ->
                navController.navigate(R.id.action_signInFragment_to_conversationsListFragment)
            LoginState.NOT_LOGGED -> {
                binding.progressBar.visibility = View.INVISIBLE
                binding.codeInputLayout.visibility = View.VISIBLE
                binding.codeInputText.setText(newState.code)
                binding.fab.isEnabled = true
            }
        }
    }

    private fun setError(error: ErrorType?) {
        when (error) {
            ErrorType.WRONG_PHONE ->
                binding.phoneInputText.error = resources.getText(R.string.wrong_phone)
            ErrorType.PHONE_NO_CONNECTION ->
                binding.phoneInputText.error = resources.getText(R.string.no_connection)
            ErrorType.EMPTY_CODE ->
                binding.codeInputLayout.error = resources.getString(R.string.empty_code)
            ErrorType.WRONG_CODE ->
                binding.codeInputLayout.error = resources.getString(R.string.wrong_code)
            ErrorType.CODE_NO_CONNECTION ->
                binding.codeInputLayout.error = resources.getText(R.string.no_connection)
            null -> {
                binding.phoneInputText.error = null
                binding.codeInputLayout.error = null
            }
        }
    }

    private fun submitCode() {
        if (viewModel.state.verificationId != null) {
            val code = binding.codeInputText.text.toString()
            if (code.isNotEmpty()) {
                val verificationId = viewModel.state.verificationId!!
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                viewModel.addAction(SignInAction(credential, code))
            } else {
                viewModel.setStateAsync { copy(error = ErrorType.EMPTY_CODE) }
            }
        }
    }

    private fun submitPhoneNumber() {
        val phoneNumber = binding.phoneInputText.text.toString()
        if (phoneNumber.isNotEmpty() && isValidPhone(phoneNumber)) {
            viewModel.setStateAsync {
                copy(phoneNumber = phoneNumber, state = LoginState.PHONE_SENT)
            }
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                requireActivity(),
                callbacks
            )
        } else {
            viewModel.setStateAsync {
                copy(
                    phoneNumber = phoneNumber,
                    error = ErrorType.WRONG_PHONE
                )
            }
        }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("SignInFragment", "onVerificationCompleted: $credential")
            viewModel.addAction(SignInAction(credential, credential.smsCode))
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d("SignInFragment", "onCodeSent: $verificationId")
            viewModel.setStateAsync {
                copy(
                    verificationId = verificationId,
                    state = LoginState.VERIFICATION_ID_RECEIVED,
                    error = null
                )
            }
        }

        // This callback is invoked in an invalid request for verification is made,
        // for instance if the the phone number format is not valid.
        //FirebaseAuthInvalidCredentialsException - Invalid request
        //FirebaseTooManyRequestsException - The SMS quota for the project has been exceeded
        override fun onVerificationFailed(e: FirebaseException) {
            Log.w("SignInFragment", "onVerificationFailed", e)
            val error = when (e) {
                is FirebaseNetworkException -> ErrorType.PHONE_NO_CONNECTION
                is FirebaseAuthInvalidCredentialsException -> ErrorType.WRONG_PHONE
                else -> null
            }
            viewModel.setStateAsync {
                copy(state = LoginState.INITIAL, error = error)
            }
        }
    }
}