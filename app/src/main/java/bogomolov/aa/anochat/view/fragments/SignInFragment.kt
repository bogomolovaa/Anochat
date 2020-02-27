package bogomolov.aa.anochat.view.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSignInBinding
import bogomolov.aa.anochat.viewmodel.LoginState
import bogomolov.aa.anochat.viewmodel.SignInViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class SignInFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: SignInViewModel by activityViewModels { viewModelFactory }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentSignInBinding>(
            inflater,
            R.layout.fragment_sign_in,
            container,
            false
        )
        binding.viewModel = viewModel
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        viewModel.loginStateLiveData.observe(viewLifecycleOwner){
            if(it == LoginState.LOGGED){
                navController.navigate(R.id.conversationsListFragment)
            }else{
                binding.passwordInputLayout.error = getString(R.string.wrong_password)
            }
        }

        return binding.root
    }

}
