package bogomolov.aa.anochat.features.conversations.dialog

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.shared.*
import dagger.hilt.android.AndroidEntryPoint

@Composable
fun SendMediaView(navController: NavController) {
    val viewModel = hiltViewModel<ConversationViewModel>(navController.getBackStackEntry("conversationRoute"))
    val state = viewModel.state.collectAsState()
    Content(state.value, viewModel, navController)
}

@Composable
private fun Content(state: DialogUiState, viewModel: ConversationViewModel, navController: NavController) {
    val context = LocalContext.current
    MaterialTheme(
        colors = LightColorPalette
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = if (state.isVideo) R.string.send_media_video else R.string.send_media_image)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
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
                                IconButton(onClick = {
                                    submit(
                                        viewModel,
                                        context,
                                        navController,
                                        state.resized,
                                        state.isVideo,
                                        text
                                    )
                                }) {
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

private fun submit(
    viewModel: ConversationViewModel,
    context: Context,
    navController: NavController,
    resized: BitmapWithName,
    isVideo: Boolean,
    text: String
) {
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
        playMessageSound(context)
        navController.popBackStack()
    } else {
        Toast.makeText(context, "Video is processing", Toast.LENGTH_LONG).show()
    }
}