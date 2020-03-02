package bogomolov.aa.anochat.view.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSendMediaBinding
import bogomolov.aa.anochat.viewmodel.SendMediaViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class SendMediaFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: SendMediaViewModel by activityViewModels { viewModelFactory }
    lateinit var mediaLocation: String
    var conversationId = 0L

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentSendMediaBinding>(
            inflater,
            R.layout.fragment_send_media,
            container,
            false
        )
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        mediaLocation = arguments?.getString("location")!!
        conversationId = arguments?.getLong("conversationId")!!

        return binding.root
    }

}
