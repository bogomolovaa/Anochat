package bogomolov.aa.anochat.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentSignInBinding
import bogomolov.aa.anochat.features.shared.ErrorType
import bogomolov.aa.anochat.features.shared.SignInError
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment : Fragment(), UpdatableView<SignInUiState> {
    val viewModel: SignInViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var binding: FragmentSignInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentSignInBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = findNavController()
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
            LoginState.PHONE_SUBMITTED -> {
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
            LoginState.CODE_SUBMITTED -> {
                binding.codeInputLayout.visibility = View.VISIBLE
                binding.codeInputText.setText(newState.code)
                binding.fab.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
            }
            LoginState.LOGGED ->
                navController.navigate(R.id.conversationsListFragment)
            LoginState.NOT_LOGGED -> {
                binding.progressBar.visibility = View.INVISIBLE
                binding.codeInputLayout.visibility = View.VISIBLE
                binding.codeInputText.setText(newState.code)
                binding.fab.isEnabled = true
            }
        }
    }

    private fun setError(error: SignInError?) {
        if (error == null) {
            binding.phoneInputText.error = null
            binding.codeInputLayout.error = null
            return
        }
        when (error.message) {
            ErrorType.WRONG_PHONE.toString() ->
                binding.phoneInputText.error = resources.getText(R.string.wrong_phone)
            ErrorType.PHONE_NO_CONNECTION.toString() ->
                binding.phoneInputText.error = resources.getText(R.string.no_connection)
            ErrorType.EMPTY_CODE.toString() ->
                binding.codeInputLayout.error = resources.getString(R.string.empty_code)
            ErrorType.WRONG_CODE.toString() ->
                binding.codeInputLayout.error = resources.getString(R.string.wrong_code)
            ErrorType.CODE_NO_CONNECTION.toString() ->
                binding.codeInputLayout.error = resources.getText(R.string.no_connection)
            else -> {
                binding.codeInputLayout.error = error.message
            }
        }
    }

    private fun submitCode() {
        val code = binding.codeInputText.text.toString()
        viewModel.addAction(SubmitSmsCodeAction(code))
    }

    private fun submitPhoneNumber() {
        val phoneNumber = binding.phoneInputText.text.toString()
        viewModel.addAction(SubmitPhoneNumberAction(phoneNumber) { requireActivity() })
    }
}