package bogomolov.aa.anochat.features.conversations.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.shared.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendMediaFragment : Fragment() {
    private val viewModel: ConversationViewModel by hiltNavGraphViewModels(R.id.dialog_graph)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            val mediaPath = arguments?.getString("path")
            val mediaUri = arguments?.getParcelable("uri") as Uri?
            val isVideo = isVideo(mediaUri)
            viewModel.resizeMedia(mediaUri, mediaPath, isVideo)
            setContent {
                val state = viewModel.state.collectAsState()
                Content(state.value)
            }
        }


    @Composable
    private fun Content(state: DialogUiState) {
        // binding.messageInputText.requestFocus()
        MaterialTheme(
            colors = LightColorPalette
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = if (state.isVideo) R.string.send_media_video else R.string.send_media_image)) },
                        navigationIcon = {
                            IconButton(onClick = {
                                findNavController().popBackStack()
                            }) {
                                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                },
                content = {
                    val showLoading = state.isVideo && state.progress < 0.98
                    if (showLoading)
                        LinearProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(top = if (showLoading) 16.dp else 0.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (state.resized != null) {
                            if (state.resized.bitmap != null) {
                                Image(
                                    bitmap = state.resized.bitmap.asImageBitmap(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 60.dp),
                                    contentScale = ContentScale.FillWidth,
                                    contentDescription = ""
                                )
                            }
                            var text by remember { mutableStateOf("") }
                            TextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = { Text(text = stringResource(id = R.string.enter_message)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.White),
                                trailingIcon = {
                                    IconButton(onClick = { submit(state.resized, state.isVideo, text) }) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "")
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    private fun submit(resized: BitmapWithName, isVideo: Boolean, text: String) {
        if (resized.processed) {
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
            findNavController().popBackStack()
        } else {
            Toast.makeText(requireContext(), "Video is processing", Toast.LENGTH_LONG).show()
        }
    }

    private fun isVideo(mediaUri: Uri?) =
        mediaUri?.let {
            it.toString().contains("document/video") ||
                    (requireContext().contentResolver.getType(mediaUri)?.startsWith("video")
                        ?: false) ||
                    mediaUri.toString().endsWith(".mp4")
        } ?: false
}