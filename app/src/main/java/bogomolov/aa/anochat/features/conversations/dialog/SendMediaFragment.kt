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
import bogomolov.aa.anochat.features.shared.nameToImage
import bogomolov.aa.anochat.features.shared.nameToVideo
import bogomolov.aa.anochat.features.shared.playMessageSound
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


        val isVideo = mediaUri?.let {
            requireContext().contentResolver.getType(mediaUri)?.startsWith("video")
        } ?: false

        (activity as AppCompatActivity).setTitle(
            if (isVideo) R.string.send_media_video else R.string.send_media_image
        )


        val resized = if (isVideo) fileStore.resizeVideo(mediaUri!!)
        else fileStore.resizeImage(mediaUri, mediaPath, toGallery = (mediaUri == null))
        if (resized != null) {
            binding.imageView.setImageBitmap(resized.bitmap)
            binding.messageInputLayout.setEndIconOnClickListener {
                val text = binding.messageInputText.text?.toString() ?: ""
                if (isVideo) {
                    viewModel.addAction(
                        SendMessageAction(video = nameToVideo(resized.name), text = text)
                    )
                } else {
                    viewModel.addAction(
                        SendMessageAction(image = nameToImage(resized.name), text = text)
                    )
                }
                playMessageSound(requireContext())
                navController.popBackStack()
            }
        }else{
            navController.popBackStack()
        }
        binding.messageInputText.requestFocus()
    }
}