package bogomolov.aa.anochat.view.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSignInBinding
import bogomolov.aa.anochat.viewmodel.LoginState
import bogomolov.aa.anochat.viewmodel.SignInViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.android.support.AndroidSupportInjection
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SignInFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: SignInViewModel by activityViewModels { viewModelFactory }
    lateinit var navController: NavController
    private lateinit var binding: FragmentSignInBinding

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_sign_in,
            container,
            false
        )
        //setHasOptionsMenu(true)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        viewModel.loginStateLiveData.observe(viewLifecycleOwner) {
            binding.codeInputLayout.visibility =
                if (it == LoginState.CODE_SENT) View.VISIBLE else View.INVISIBLE
            if(it == LoginState.LOGGED)
                navController.navigate(R.id.conversationsListFragment)
        }


        binding.fab.setOnClickListener {
            when (viewModel.loginStateLiveData.value ?: LoginState.NOT_LOGGED) {
                LoginState.NOT_LOGGED -> {
                    val phoneNumber = binding.phoneInputText.text.toString()
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        phoneNumber,
                        60,
                        TimeUnit.SECONDS,
                        requireActivity(),
                        callbacks
                    )
                    binding.codeInputLayout.visibility = View.VISIBLE
                    viewModel.phoneNumber = phoneNumber
                }
                LoginState.CODE_SENT -> {
                    if (viewModel.verificationId != null) {
                        val code = binding.codeInputText.text.toString()
                        val credential =
                            PhoneAuthProvider.getCredential(viewModel.verificationId!!, code)
                        viewModel.signIn(credential)
                    } else {
                        Log.i("test", "viewModel.verificationId null")
                    }
                }
            }


        }

        return binding.root
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("test", "onVerificationCompleted:$credential")

            viewModel.signIn(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w("test", "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                // ...
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                // ...
            }
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d("test", "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            viewModel.verificationId = verificationId
            viewModel.loginStateLiveData.value = LoginState.CODE_SENT
        }
    }


    /*
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.signup_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_to_sign_up) navController.navigate(R.id.signUpFragment)
        return true
    }
     */
}
