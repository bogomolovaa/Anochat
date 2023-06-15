package bogomolov.aa.anochat.features.conversations.dialog

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "ConversationView"

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun ConversationView(conversationId: Long, uri: Uri? = null) {
    val navController = LocalNavController.current
    val route = remember {
        navController!!.getBackStackEntry("conversationRoute")
    }
    val viewModel =
        hiltViewModel<ConversationViewModel>(route)
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    LaunchedEffect(0) {
        viewModel.initConversation(conversationId)
        if (uri != null && viewModel.uri != uri)
            navigateToSendMediaFragment(
                viewModel = viewModel,
                context = context,
                uri = uri,
                navController = navController
            )
        viewModel.uri = uri
    }

    EventHandler(viewModel.events) {
        when (it) {
            is OnMessageSent -> keyboardController?.hide()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            keyboardController?.hide()
            if (viewModel.currentState.inputState == InputStates.FAB_EXPAND)
                viewModel.updateState { copy(inputState = InputStates.INITIAL) }
        }
    })
    val state = viewModel.state.collectAsState()
    Content(state.value, viewModel)
}

@Preview
@ExperimentalMaterialApi
@Composable
private fun Content(
    state: DialogUiState = testDialogUiState,
    viewModel: ConversationViewModel? = null
) {
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        UserNameLayout(
                            state = state,
                            onClick = { navigateToUserFragment(viewModel, navController) }
                        )
                        if (state.selectedMessages.isNotEmpty()) {
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { viewModel?.deleteMessages() }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Clear"
                                )
                            }
                            IconButton(
                                onClick = { viewModel?.clearMessages() }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(start = 4.dp, end = 4.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        MessagesList(state.pagingDataFlow, state, viewModel)
                        ReplyLayout(state, viewModel)
                    }
                    Row(Modifier.padding(end = 64.dp)) {
                        InputLayout(state, viewModel)
                    }
                }
                FabsLayout(state, viewModel)
            }
        }
    )
}

@Composable
private fun InputLayout(state: DialogUiState = testDialogUiState, viewModel: ConversationViewModel? = null) {
    val playingState = state.playingState
    MaterialTheme(colors = LightColorPalette) {
        ConversationInput(
            state = state,
            playingState = playingState,
            playOnClick = { audioFile, messageId ->
                if (playingState?.paused != false) {
                    viewModel?.startPlaying(audioFile, messageId)
                } else {
                    viewModel?.pausePlaying()
                }
            },
            onTextChanged = { text ->
                viewModel?.textChanged(text)
            },
            onClear = {
                viewModel?.updateState { copy(inputState = InputStates.INITIAL, audioFile = null) }
            },
            emojiOnClick = {

            }
        )
    }
}

@Composable
private fun ReplyLayout(state: DialogUiState = testDialogUiState, viewModel: ConversationViewModel?) {
    val replyMessage = state.replyMessage
    val playingState = state.playingState
    val context = LocalContext.current
    if (replyMessage != null) {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        if (replyMessage.image != null || replyMessage.video != null)
            LaunchedEffect(replyMessage.id) {
                withContext(Dispatchers.IO) {
                    val image = if (replyMessage.image != null) replyMessage.image
                    else videoThumbnail(replyMessage.video!!)
                    bitmap = getBitmapFromGallery(image, context, 4)
                }
            }
        Card(
            modifier = Modifier.padding(bottom = 4.dp),
            shape = RoundedCornerShape(6.dp),
            elevation = 1.dp,
            backgroundColor = colorResource(id = R.color.time_message_color)
        ) {
            ReplyMessage(
                message = replyMessage,
                bitmap = bitmap,
                replyPlayingState = if (playingState?.messageId == replyMessage.messageId) playingState else null,
                playOnClick = { audioFile, messageId ->
                    if (playingState?.paused != false) {
                        viewModel?.startPlaying(audioFile, messageId)
                    } else {
                        viewModel?.pausePlaying()
                    }
                },
                onClear = {
                    viewModel?.updateState { copy(replyMessage = null) }
                }
            )
        }
    }
}

@Composable
private fun FabsLayout(
    state: DialogUiState = testDialogUiState,
    viewModel: ConversationViewModel?
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val fileChooser = rememberLauncherForActivityResult(StartFileChooser()) { uri ->
        navigateToSendMediaFragment(viewModel = viewModel, context = context, uri = uri, navController = navController)
    }
    val takePicture = rememberLauncherForActivityResult(TakePictureFromCamera()) {
        navigateToSendMediaFragment(
            viewModel = viewModel,
            context = context,
            path = viewModel!!.currentState.photoPath,
            navController = navController
        )
    }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) fileChooser.launch(Unit)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            val photoFile = context.createTempImageFile()
            viewModel?.updateState { copy(photoPath = photoFile.path) }
            takePicture.launch(photoFile)
        }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel?.startRecording()
    }

    InputFabs(
        state = state,
        onClick = {
            fabOnClick(context, state.inputState, viewModel)
        },
        onVoice = {
            microphonePermission.launch(RECORD_AUDIO)
        },
        onCamera = {
            viewModel?.updateState { copy(inputState = InputStates.INITIAL) }
            cameraPermission.launch(CAMERA)
        },
        onGallery = {
            viewModel?.updateState { copy(inputState = InputStates.INITIAL) }
            readPermission.launch(READ_EXTERNAL_STORAGE)
        }
    )
}

@ExperimentalMaterialApi
@Composable
private fun MessagesList(
    pagingDataFlow: Flow<PagingData<MessageViewData>>? = null,
    state: DialogUiState = testDialogUiState,
    viewModel: ConversationViewModel? = null
) {
    if (pagingDataFlow != null) {
        val lazyPagingItems = pagingDataFlow.collectAsLazyPagingItems()
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = true,
        ) {
            items(count = lazyPagingItems.itemCount) { index ->
                lazyPagingItems[index]?.let {
                    ShowMessage(
                        messageData = it,
                        selected = state.selectedMessages.contains(it.message),
                        isScrolling = listState.isScrollInProgress,
                        playingState = state.playingState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@Composable
private fun ShowMessage(
    messageData: MessageViewData?,
    selected: Boolean = false,
    isScrolling: Boolean = false,
    playingState: PlayingState?,
    viewModel: ConversationViewModel?
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    if (messageData != null)
        LaunchedEffect(messageData.message.id) {
            viewModel?.messageDisplayed(messageData)
        }
    var loading by remember { mutableStateOf(true) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var replyBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val messageThumbnail = messageData?.message?.getThumbnail()
    val replyMessageThumbnail = messageData?.message?.replyMessage?.getThumbnail()
    if (messageThumbnail != null || replyMessageThumbnail != null) {
        LaunchedEffect(messageData.message) {
            withContext(Dispatchers.IO) {
                if (isScrolling) delay(300)
                messageThumbnail?.let { bitmap = getBitmapFromGallery(it, context, 1) }
                replyMessageThumbnail?.let { replyBitmap = getBitmapFromGallery(it, context, 8) }
                loading = false
            }
        }
        DisposableEffect(messageData.message.id) {
            onDispose {
                bitmap = null
                replyBitmap = null
            }
        }
    }
    if (messageData != null) {
        messageData.playingState =
            if (playingState?.messageId == messageData.message.messageId) playingState else null
        messageData.replyPlayingState =
            if (playingState?.messageId == messageData.message.replyMessage?.messageId) playingState else null
    }
    MessageCompose(
        data = messageData,
        selected = selected,
        loadingBitmaps = loading,
        bitmap = bitmap,
        replyBitmap = replyBitmap,
        onClick = {
            when {
                messageData?.message?.video != null -> videoOnClick(messageData.message, context, navController)
                messageData?.message?.image != null -> imageOnClick(messageData.message, navController)
            }
        },
        onSelect = {
            messageData?.message?.let {
                viewModel?.selectMessage(it)
            }
        },
        onSwipe = {
            messageData?.message?.let {
                viewModel?.updateState { copy(replyMessage = it) }
            }
        },
        playOnClick = { audioFile: String?, messageId: String? ->
            if (playingState?.paused != false) {
                viewModel?.startPlaying(audioFile, messageId)
            } else {
                viewModel?.pausePlaying()
            }
        }
    )
}

private fun Message.getThumbnail() = image ?: video?.let { videoThumbnail(it) }

private fun fabOnClick(context: Context, inputState: InputStates, viewModel: ConversationViewModel?) {
    when (inputState) {
        InputStates.INITIAL -> viewModel?.updateState { copy(inputState = InputStates.FAB_EXPAND) }
        InputStates.FAB_EXPAND -> viewModel?.updateState { copy(inputState = InputStates.INITIAL) }
        InputStates.TEXT_ENTERED -> {
            viewModel?.sendMessage(SendMessageData(text = viewModel.currentState.text))
            playMessageSound(context)
        }
        InputStates.VOICE_RECORDED -> {
            viewModel?.sendMessage(SendMessageData(audio = viewModel.currentState.audioFile))
            playMessageSound(context)
        }
        InputStates.VOICE_RECORDING -> viewModel?.stopRecording()
    }
}

private fun videoOnClick(message: Message, context: Context, navController: NavController?) {
    if (message.received == 1 || message.isMine) {
        val uriWithSource = getUriWithSource(message.video!!, context)
        if (uriWithSource.uri != null) navController?.navigate("video?uri=${uriWithSource.uri}")
    }
}

private fun imageOnClick(message: Message, navController: NavController?) {
    if (message.received == 1 || message.isMine)
        navController?.navigate("image?name=${message.image}")
}

private fun navigateToUserFragment(viewModel: ConversationViewModel?, navController: NavController?) {
    val userId = viewModel?.currentState?.conversation?.user?.id
    if (userId != null) navController?.navigate("user/$userId")
}

private fun navigateToSendMediaFragment(
    context: Context,
    viewModel: ConversationViewModel?,
    uri: Uri? = null,
    path: String? = null,
    navController: NavController?
) {
    val isVideo = context.isVideo(uri)
    viewModel?.resizeMedia(uri, path, isVideo)
    navController?.navigate("media")
}

@SuppressLint("SimpleDateFormat")
private fun Context.createTempImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date());
    val imageFileName = "JPEG_" + timeStamp + "_";
    val storageDir = filesDir
    return File.createTempFile(imageFileName, ".jpg", storageDir).apply { deleteOnExit() }
}

private class StartFileChooser : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        return Intent.createChooser(intent, context.getString(R.string.select_file))
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.data
}

private class TakePictureFromCamera : ActivityResultContract<File, Unit>() {
    override fun createIntent(context: Context, photoFile: File): Intent {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoUri = FileProvider.getUriForFile(
            context,
            "bogomolov.aa.anochat.fileprovider",
            photoFile
        )
        Log.d(TAG, "photoUri $photoUri")
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        return takePictureIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {

    }
}


private fun Context.isVideo(mediaUri: Uri?) =
    mediaUri?.let {
        it.toString().contains("document/video") ||
                (contentResolver.getType(mediaUri)?.startsWith("video")
                    ?: false) ||
                mediaUri.toString().endsWith(".mp4")
    } ?: false

/*
        //todo: setEmojiSizeRes
        if (text.length in 1..2 && Character.isSurrogate(text[0])) {
            binding.messageText.setEmojiSizeRes(R.dimen.message_one_emoji_size)
        } else {
            binding.messageText.setEmojiSizeRes(R.dimen.message_emoji_size)
        }

        //todo: messages menu
        data.actionsMap[R.id.delete_messages_action] = { _, items ->
            viewModel.deleteMessages(items.map { it.message.id }.toSet())
        }
        data.actionsMap[R.id.reply_message_action] = { _, items ->
            if (items.isNotEmpty()) {
                val message = items.last()
                onReply(message.message)
            }
        }
        data.actionsMap[R.id.copy_messages_action] = { _, items ->
            val context = fragment.requireContext()
            val text = items.first().message.text
            val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", text)
            clipBoard.setPrimaryClip(clipData)
            val messageText = context.resources.getString(R.string.copied)
            Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
        }
     */