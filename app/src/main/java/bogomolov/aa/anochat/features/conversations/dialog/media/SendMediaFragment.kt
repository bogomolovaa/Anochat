package bogomolov.aa.anochat.features.conversations.dialog.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSendMediaBinding
import bogomolov.aa.anochat.repository.getFilePath
import bogomolov.aa.anochat.repository.resizeImage
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class SendMediaFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: SendMediaViewModel by activityViewModels { viewModelFactory }
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
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        val mediaPath = arguments?.getString("path")
        val mediaUri = arguments?.getParcelable("uri") as Uri?
        conversationId = arguments?.getLong("conversationId")!!

        val resizedImage = if(mediaUri!=null)
            resizeImage(mediaUri, requireContext())
        else
            resizeImage(mediaPath!!, requireContext())
        binding.imageView.setImageBitmap(BitmapFactory.decodeFile(getFilePath(requireContext(), resizedImage)))
        binding.messageInputLayout.setEndIconOnClickListener {
            val text = binding.messageInputText.text?.toString() ?: ""
            viewModel.sendMessage(resizedImage, text, conversationId)
            navController.popBackStack()
        }

        return binding.root
    }

}
