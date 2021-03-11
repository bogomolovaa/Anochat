package bogomolov.aa.anochat.features.conversations.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentSendMediaBinding
import bogomolov.aa.anochat.repository.FileStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SendMediaFragment : Fragment() {
    private val viewModel: ConversationViewModel by hiltNavGraphViewModels(R.id.dialog_graph)
    private var conversationId = 0L
    private lateinit var binding: FragmentSendMediaBinding

    @Inject
    lateinit var fileStore: FileStore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentSendMediaBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        val mediaPath = arguments?.getString("path")
        val mediaUri = arguments?.getParcelable("uri") as Uri?
        conversationId = arguments?.getLong("conversationId")!!

        val resizedImage =
            fileStore.resizeImage(mediaUri, mediaPath, toGallery = (mediaUri == null))
        if (resizedImage != null) {
            binding.imageView.setImageBitmap(resizedImage.bitmap)
            binding.messageInputLayout.setEndIconOnClickListener {
                val text = binding.messageInputText.text?.toString() ?: ""
                viewModel.addAction(SendMessageAction(image = resizedImage.name, text = text))
                navController.popBackStack()
            }
        }
    }
}