package bogomolov.aa.anochat.features.conversations.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentSendMediaBinding
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.resizeImage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendMediaFragment : Fragment() {
    private val viewModel: ConversationViewModel by hiltNavGraphViewModels(R.id.dialog_graph)
    private var conversationId = 0L
    private lateinit var binding: FragmentSendMediaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSendMediaBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        val mediaPath = arguments?.getString("path")
        val mediaUri = arguments?.getParcelable("uri") as Uri?
        conversationId = arguments?.getLong("conversationId")!!

        val saveToGallery = Settings.get(Settings.GALLERY, requireContext()) && mediaUri == null
        val resizedImage =
            resizeImage(mediaUri, mediaPath, requireContext(), toGallery = saveToGallery)
        if (resizedImage != null) {
            binding.imageView.setImageBitmap(resizedImage.bitmap)
            binding.messageInputLayout.setEndIconOnClickListener {
                val text = binding.messageInputText.text?.toString() ?: ""
                viewModel.addAction(SendMessageAction(image = resizedImage.name, text = text))
                navController.popBackStack()
            }
        }
        return binding.root
    }
}