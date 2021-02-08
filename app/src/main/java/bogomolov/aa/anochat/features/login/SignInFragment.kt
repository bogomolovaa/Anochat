package bogomolov.aa.anochat.features.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSignInBinding
import bogomolov.aa.anochat.features.shared.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.UpdatableView
import bogomolov.aa.anochat.repository.isValidPhone
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.android.support.AndroidSupportInjection
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SignInFragment : Fragment(), UpdatableView<SignInUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: SignInViewModel by viewModels { viewModelFactory }
    private lateinit var navController: NavController
    private lateinit var binding: FragmentSignInBinding

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_sign_in,
            container,
            false
        )
        binding.lifecycleOwner = this
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        binding.toolbar.navigationIcon = null

        binding.fab.setOnClickListener {
            when (viewModel.currentState.state) {
                LoginState.NOT_LOGGED -> verifyPhoneNumber()
                else -> submitCode()
            }
        }

        return binding.root
    }

    private fun submitCode() {
        if (viewModel.currentState.verificationId != null) {
            val code = binding.codeInputText.text.toString()
            if (code.isNotEmpty()) {
                val verificationId = viewModel.currentState.verificationId!!
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                viewModel.addAction(SignInAction(credential))
            } else {
                binding.codeInputLayout.error = resources.getString(R.string.empty_code)
            }
        } else {
            Log.i("test", "viewModel.verificationId null")
        }
    }

    private fun verifyPhoneNumber() {
        val phoneNumber = binding.phoneInputText.text.toString()
        if (phoneNumber.isNotEmpty() && isValidPhone(phoneNumber)) {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                requireActivity(),
                callbacks
            )
            binding.codeInputLayout.visibility = View.VISIBLE
            viewModel.setStateAsync { copy(phoneNumber = phoneNumber) }
        } else {
            binding.phoneInputLayout.error =
                resources.getString(R.string.enter_valid_phone)
        }
    }

    override fun updateView(newState: SignInUiState, currentState: SignInUiState) {
        Log.i("SignInFragment", "updateView newState:\n$newState")
        binding.phoneInputText.setText(newState.phoneNumber)
        binding.codeInputLayout.visibility =
            if (newState.state == LoginState.CODE_SENT) View.VISIBLE else View.INVISIBLE
        if (newState.state == LoginState.LOGGED)
            navController.navigate(R.id.action_signInFragment_to_conversationsListFragment)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("SignInFragment", "onVerificationCompleted: $credential")
            viewModel.addAction(SignInAction(credential))
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d("SignInFragment", "onCodeSent: $verificationId")
            viewModel.setStateAsync {
                copy(verificationId = verificationId, state = LoginState.CODE_SENT)
            }
        }

        // This callback is invoked in an invalid request for verification is made,
        // for instance if the the phone number format is not valid.
        //FirebaseAuthInvalidCredentialsException - Invalid request
        //FirebaseTooManyRequestsException - The SMS quota for the project has been exceeded
        override fun onVerificationFailed(e: FirebaseException) {
            Log.w("SignInFragment", "onVerificationFailed", e)
        }
    }

}
