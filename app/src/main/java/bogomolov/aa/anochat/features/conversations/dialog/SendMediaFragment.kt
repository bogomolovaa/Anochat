package bogomolov.aa.anochat.features.conversations.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentSendMediaBinding
import bogomolov.aa.anochat.features.shared.nameToImage
import bogomolov.aa.anochat.features.shared.nameToVideo
import bogomolov.aa.anochat.features.shared.playMessageSound
import bogomolov.aa.anochat.repository.FileStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
            it.toString().contains("document/video") ||
                    (requireContext().contentResolver.getType(mediaUri)?.startsWith("video")
                        ?: false) ||
                    mediaUri.toString().endsWith(".mp4")
        } ?: false

        (activity as AppCompatActivity).setTitle(
            if (isVideo) R.string.send_media_video else R.string.send_media_image
        )

        val resized = if (isVideo) fileStore.resizeVideo(mediaUri!!, lifecycleScope)
        else fileStore.resizeImage(mediaUri, mediaPath, toGallery = (mediaUri == null))
        if (resized != null) {
            if (isVideo) {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.max = 100
                lifecycleScope.launch {
                    resized.progress.collect {
                        binding.progressBar.progress = it
                        if (it > 98) {
                            binding.progressBar.visibility = View.INVISIBLE
                            cancel()
                        }
                    }
                }
            } else {
                binding.progressBar.visibility = View.GONE
            }
            binding.imageView.setImageBitmap(resized.bitmap)
            binding.messageInputLayout.setEndIconOnClickListener {
                if (resized.processed) {
                    val text = binding.messageInputText.text?.toString() ?: ""
                    if (isVideo) {
                        viewModel.sendMessage(
                            SendMessageData(video = nameToVideo(resized.name), text = text)
                        )

                    } else {
                        viewModel.sendMessage(
                            SendMessageData(image = nameToImage(resized.name), text = text)
                        )
                    }
                    playMessageSound(requireContext())
                    navController.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Video is processing", Toast.LENGTH_LONG)
                        .show()
                }
            }
        } else {
            navController.popBackStack()
        }
        binding.messageInputText.requestFocus()
    }
}