package bogomolov.aa.anochat.features.conversations.dialog

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.main.Route
import bogomolov.aa.anochat.features.main.theme.MyTopAppBar
import bogomolov.aa.anochat.features.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "ConversationView"

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun ConversationView(conversationId: Long, uri: String? = null) {
    val navController = LocalNavController.current
    val route = remember { navController!!.getBackStackEntry(Route.Conversation.navGraphRoute) }
    val viewModel = hiltViewModel<ConversationViewModel>(route)
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val emojiKeyboardOpened = remember { mutableStateOf(false) }
    val keyboardState by keyboardAsState {
        if (it) emojiKeyboardOpened.value = false
    }
    BackHandler(enabled = emojiKeyboardOpened.value) {
        emojiKeyboardOpened.value = false
    }
    val navigateToSendMedia: (Uri?) -> Unit = remember {
        {
            val isVideo = context.isVideo(it)
            viewModel.resizeMedia(it, isVideo)
            navController?.navigate(Route.Media.route)
        }
    }
    LaunchedEffect(0) {
        viewModel.initConversation(conversationId)
        if (uri != null && viewModel.uri != uri) navigateToSendMedia(uri.toUri())
        viewModel.uri = uri
    }
    val lazyListState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                keyboardController?.hide()
                if (viewModel.currentState.inputState.state == InputState.State.FAB_EXPAND)
                    viewModel.resetInputState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    viewModel.events.collectEvents {
        when (it) {
            is OnMessageSent -> {
                playMessageSound(context)
                while (lazyListState.firstVisibleItemIndex != 0) lazyListState.animateScrollToItem(0)
                keyboardController?.hide()
                emojiKeyboardOpened.value = false
            }
        }
    }
    viewModel.state.collectState {
        Content(
            state = it,
            lazyListState = lazyListState,
            keyboardState = keyboardState,
            emojiKeyboardOpened = emojiKeyboardOpened,
            viewModel = viewModel,
            navigateToSendMedia = navigateToSendMedia
        )
    }
}

@ExperimentalMaterial3Api
@Composable
private fun Content(
    state: DialogState = testDialogUiState,
    lazyListState: LazyListState = rememberLazyListState(),
    keyboardState: KeyboardState = KeyboardState(),
    emojiKeyboardOpened: MutableState<Boolean> = mutableStateOf(false),
    viewModel: ConversationViewModel? = null,
    navigateToSendMedia: (Uri?) -> Unit = { }
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val play: (String?, String?) -> Unit =
        remember { { audioFile, messageId -> viewModel?.play(audioFile, messageId) } }
    Scaffold(
        topBar = {
            MyTopAppBar(
                title = {
                    state.conversation?.let {
                        UserNameLayout(
                            userStatus = state.userStatus,
                            conversation = it,
                            onClick = remember { { navController?.navigate(Route.User.route(it.user.id)) } }
                        )
                    }
                },
                actions = {
                    if (state.selectedMessages.isNotEmpty()) {
                        IconButton(
                            onClick = remember { { viewModel?.deleteMessages() } }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Clear"
                            )
                        }
                        IconButton(
                            onClick = remember { { viewModel?.clearMessages() } }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = remember { { navController?.popBackStack() } }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val emojiKeyboardVisible = emojiKeyboardOpened.value && !keyboardState.opened
        ConstraintLayout(
            createInsetsModifier(padding)
                .fillMaxSize()
        ) {
            val (messages, reply, input, fab, emojiKeyboard) = createRefs()
            state.messagesFlow?.let {
                MessagesList(
                    modifier = Modifier.constrainAs(messages) {
                        top.linkTo(parent.top)
                        bottom.linkTo(input.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    messagesFlow = it,
                    lazyListState = lazyListState,
                    selectedMessages = state.selectedMessages,
                    playingState = state.playingState,
                    play = play,
                    messageDisplayed = remember { { viewModel?.messageDisplayed(it) } },
                    selectMessage = remember { { viewModel?.selectMessage(it) } },
                    setReplyMessage = remember { { viewModel?.setReplyMessage(it) } },
                    messageOnClick = remember {
                        {
                            when {
                                it.video != null -> videoOnClick(it, context, navController)
                                it.image != null -> imageOnClick(it, navController)
                            }
                        }
                    }
                )
            }
            state.replyMessage?.let {
                ReplyLayout(
                    modifier = Modifier.constrainAs(reply) {
                        bottom.linkTo(input.top)
                        start.linkTo(parent.start)
                    },
                    replyMessage = it,
                    playingState = state.playingState,
                    play = play,
                    clear = remember { { viewModel?.clearReplyMessage() } }
                )
            }
            ConversationInput(
                modifier = Modifier.constrainAs(input) {
                    bottom.linkTo(if (emojiKeyboardVisible) emojiKeyboard.top else parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(fab.start)
                    width = Dimension.fillToConstraints
                },
                inputState = state.inputState,
                playingState = state.playingState,
                emojiKeyboardOpened = emojiKeyboardOpened,
                isKeyboardOpened = keyboardState.opened,
                playOnClick = remember { { audioFile, messageId -> viewModel?.play(audioFile, messageId) } },
                onTextChanged = remember { { viewModel?.textChanged(it) } },
                onClear = remember { { viewModel?.resetInputState() } }
            )
            FabsLayout(
                modifier = Modifier.constrainAs(fab) {
                    bottom.linkTo(if (emojiKeyboardVisible) emojiKeyboard.top else parent.bottom)
                    end.linkTo(parent.end)
                },
                inputState = state.inputState.state,
                setPhotoPath = remember { { viewModel?.setPhotoPath(it) } },
                fabPressed = remember { { viewModel?.fabPressed() } },
                resetInputState = remember { { viewModel?.resetInputState() } },
                startRecording = remember { { viewModel?.startRecording() } },
                navigateToSendMedia = navigateToSendMedia
            )
            if (emojiKeyboardVisible)
                AndroidView(
                    factory = { context ->
                        EmojiPickerView(context).apply {
                            setOnEmojiPickedListener {
                                viewModel?.appendText(it.emoji)
                            }
                        }
                    },
                    modifier = Modifier
                        .constrainAs(emojiKeyboard) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .heightIn(max = keyboardState.height.dp),
                )
        }
    }
}

@Preview
@Composable
private fun ReplyLayout(
    modifier: Modifier = Modifier,
    replyMessage: Message = testMessage,
    playingState: PlayingState? = null,
    play: (String?, String?) -> Unit = { _, _ -> },
    clear: () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    if (replyMessage.image != null || replyMessage.video != null)
        LaunchedEffect(replyMessage.id) {
            withContext(Dispatchers.IO) {
                val image = replyMessage.image ?: replyMessage.video?.let { videoThumbnail(it) }
                bitmap.value = getBitmapFromGallery(image, context, 4)
            }
        }
    Card(
        modifier = modifier
            .padding(bottom = 4.dp)
            .shadow(shape = MaterialTheme.shapes.small, spotColor = Color.Black, elevation = 2.dp),
        shape = MaterialTheme.shapes.small
    ) {
        ReplyMessage(
            modifier = Modifier.widthIn(min = 120.dp, max = 258.dp),
            message = replyMessage,
            bitmap = bitmap,
            replyPlayingState = if (playingState?.messageId == replyMessage.messageId) playingState else null,
            playOnClick = play,
            onClear = clear
        )
    }
}

@Composable
private fun FabsLayout(
    modifier: Modifier = Modifier,
    inputState: InputState.State = InputState.State.INITIAL,
    setPhotoPath: (String) -> Unit,
    fabPressed: () -> Unit,
    resetInputState: () -> Unit,
    startRecording: () -> Unit,
    navigateToSendMedia: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val fileChooser = rememberLauncherForActivityResult(StartFileChooser()) { uri ->
        navigateToSendMedia(uri)
    }
    val takePicture = rememberLauncherForActivityResult(TakePictureFromCamera()) {
        navigateToSendMedia(null)
    }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) fileChooser.launch(Unit)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            val photoFile = context.createTempImageFile()
            setPhotoPath(photoFile.path)
            takePicture.launch(photoFile)
        }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startRecording()
    }
    InputFabs(
        modifier = modifier,
        inputState = inputState,
        onClick = fabPressed,
        onVoice = remember {
            { microphonePermission.launch(RECORD_AUDIO) }
        },
        onCamera = remember {
            {
                resetInputState()
                cameraPermission.launch(CAMERA)
            }
        },
        onGallery = remember {
            {
                resetInputState()
                readPermission.launch(READ_EXTERNAL_STORAGE)
            }
        }
    )
}

@ExperimentalMaterial3Api
@Composable
private fun MessagesList(
    modifier: Modifier = Modifier,
    messagesFlow: ImmutableFlow<PagingData<Any>> = ImmutableFlow(flowOf(PagingData.from(listOf(testMessage)))),
    lazyListState: LazyListState = rememberLazyListState(),
    selectedMessages: ImmutableList<Message>,
    playingState: PlayingState?,
    play: (String?, String?) -> Unit,
    messageDisplayed: (Message) -> Unit,
    selectMessage: (Message) -> Unit,
    setReplyMessage: (Message) -> Unit,
    messageOnClick: (Message) -> Unit
) {
    val lazyPagingItems = messagesFlow.collectAsLazyPagingItems()
    LaunchedEffect(messagesFlow) {
        snapshotFlow { lazyPagingItems.itemCount }.collect {
            if (lazyListState.firstVisibleItemIndex == 0) lazyListState.animateScrollToItem(0)
        }
    }
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        reverseLayout = true,
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey {
                when (it) {
                    is Message -> it.id
                    is DateDelimiter -> it.time
                    else -> Unit
                }
            },
            contentType = lazyPagingItems.itemContentType { if (it is Message) 0 else 1 }
        ) { index ->
            lazyPagingItems[index]?.let {
                when (it) {
                    is Message -> ShowMessage(
                        message = it,
                        selected = selectedMessages.contains(it),
                        playingState = playingState,
                        play = play,
                        messageDisplayed = messageDisplayed,
                        selectMessage = selectMessage,
                        setReplyMessage = setReplyMessage,
                        onClick = messageOnClick
                    )
                    is DateDelimiter -> DateDelimiterCompose(it)
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun ShowMessage(
    message: Message = testMessage,
    selected: Boolean = false,
    playingState: PlayingState?,
    play: (String?, String?) -> Unit,
    messageDisplayed: (Message) -> Unit,
    selectMessage: (Message) -> Unit,
    setReplyMessage: (Message) -> Unit,
    onClick: (Message) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(message.id) {
        messageDisplayed(message)
    }
    var loading by remember { mutableStateOf(true) }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val replyBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val messageThumbnail = message.getThumbnail()
    val replyMessageThumbnail = message.replyMessage?.getThumbnail()
    if (messageThumbnail != null || replyMessageThumbnail != null) {
        LaunchedEffect(message) {
            withContext(Dispatchers.IO) {
                messageThumbnail?.let { bitmap.value = getBitmapFromGallery(it, context, 1) }
                replyMessageThumbnail?.let { replyBitmap.value = getBitmapFromGallery(it, context, 8) }
                loading = false
            }
        }
    }

    MessageCompose(
        message = message,
        playingState = if (playingState?.messageId == message.messageId) playingState else null,
        replyPlayingState = if (playingState?.messageId == message.replyMessage?.messageId) playingState else null,
        selected = selected,
        loadingBitmaps = loading,
        bitmap = bitmap,
        replyBitmap = replyBitmap,
        onClick = { onClick(message) },
        onSelect = { selectMessage(message) },
        onSwipe = { setReplyMessage(message) },
        playOnClick = play
    )
}

private fun Message.getThumbnail() = image ?: video?.let { videoThumbnail(it) }

private fun videoOnClick(message: Message, context: Context, navController: NavController?) {
    if (message.received == 1 || message.isMine)
        getUriWithSource(
            message.video!!,
            context
        ).uri?.let { navController?.navigate(Route.Video.route(it.toString())) }
}

private fun imageOnClick(message: Message, navController: NavController?) {
    if (message.received == 1 || message.isMine)
        message.image?.let { navController?.navigate(Route.Image.route(it)) }
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