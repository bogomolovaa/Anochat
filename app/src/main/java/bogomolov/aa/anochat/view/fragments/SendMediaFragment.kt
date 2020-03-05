package bogomolov.aa.anochat.view.fragments

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.resizeImage
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSendMediaBinding
import bogomolov.aa.anochat.viewmodel.SendMediaViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class SendMediaFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: SendMediaViewModel by activityViewModels { viewModelFactory }
    lateinit var mediaPath: String
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
        binding.lifecycleOwner = this
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        mediaPath = arguments?.getString("path")!!
        conversationId = arguments?.getLong("conversationId")!!

        val resizedImage = resizeImage(path = mediaPath,context = requireContext())
        binding.imageView.setImageBitmap(BitmapFactory.decodeFile(resizedImage.path))
        binding.messageInputLayout.setEndIconOnClickListener {
            val text = binding.messageInputText.text?.toString() ?: ""
            Log.i("test","send message: $text")
            viewModel.sendMessage(resizedImage.name, text, conversationId)
            navController.popBackStack()
        }

        return binding.root
    }

}
