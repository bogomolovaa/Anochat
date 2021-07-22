package bogomolov.aa.anochat.features.conversations.dialog

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.transition.Fade
import androidx.transition.Slide
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.*
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ConversationFragment"

@AndroidEntryPoint
class ConversationFragment : Fragment() {
    private val viewModel: ConversationViewModel by hiltNavGraphViewModels(R.id.dialog_graph)

    override fun onPause() {
        super.onPause()
        hideKeyBoard()
        if (viewModel.currentState.inputState == InputStates.FAB_EXPAND)
            viewModel.updateState { copy(inputState = InputStates.INITIAL) }
    }

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                val state = viewModel.state.collectAsState()
                Content(state.value)
            }
        }

    @Preview
    @ExperimentalMaterialApi
    @Composable
    private fun Content(state: DialogUiState = testDialogUiState) {
        MaterialTheme(
            colors = LightColorPalette
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            UserNameLayout(
                                state = state,
                                onClick = {
                                    val userId = viewModel.currentState.conversation?.user?.id
                                    if (userId != null) navigateToUserFragment(userId)
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                findNavController().popBackStack()
                            }) {
                                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                },
                content = {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp, end = 8.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                MessagesList(state)
                                ReplyLayout(state)
                            }
                            Row(Modifier.padding(end = 64.dp)) {
                                InputLayout(state)
                            }
                        }
                        FabsLayout(state)
                    }
                }
            )
        }
    }

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

    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val conversationId = arguments?.get("id") as Long
        viewModel.initConversation(conversationId)

        val uri = arguments?.getString("uri")?.toUri()
        if (uri != null) {
            arguments?.remove("uri")
            navigateToSendMediaFragment(uri = uri)
            return
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.events.collect {
                when (it) {
                    is OnMessageSent -> {
                        hideKeyBoard()
                    }
                }
            }
        }
    }

    @Composable
    private fun InputLayout(state: DialogUiState = testDialogUiState) {
        val playingState = state.playingState
        MaterialTheme(colors = LightColorPalette) {
            ConversationInput(
                state = state,
                playingState = playingState,
                playOnClick = { audioFile, messageId ->
                    if (playingState?.paused != false) {
                        viewModel.startPlaying(audioFile, messageId)
                    } else {
                        viewModel.pausePlaying()
                    }
                },
                onTextChanged = { text ->
                    viewModel.textChanged(text)
                },
                onClear = {
                    viewModel.updateState { copy(inputState = InputStates.INITIAL, audioFile = null) }
                },
                emojiOnClick = {

                }
            )
        }
    }

    @Composable
    private fun ReplyLayout(state: DialogUiState = testDialogUiState) {
        val replyMessage = state.replyMessage
        val playingState = state.playingState
        if (replyMessage != null) {
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
            if (replyMessage.image != null || replyMessage.video != null)
                LaunchedEffect(replyMessage.id) {
                    withContext(Dispatchers.IO) {
                        val image = if (replyMessage.image != null) replyMessage.image
                        else videoThumbnail(replyMessage.video!!)
                        bitmap = getBitmapFromGallery(image, requireContext(), 4)
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
                            viewModel.startPlaying(audioFile, messageId)
                        } else {
                            viewModel.pausePlaying()
                        }
                    },
                    onClear = {
                        viewModel.updateState { copy(replyMessage = null) }
                    }
                )
            }
        }
    }

    @Composable
    private fun FabsLayout(state: DialogUiState = testDialogUiState) {
        InputFabs(
            state = state,
            onClick = {
                when (state.inputState) {
                    InputStates.INITIAL -> viewModel.updateState { copy(inputState = InputStates.FAB_EXPAND) }
                    InputStates.FAB_EXPAND -> viewModel.updateState { copy(inputState = InputStates.INITIAL) }
                    InputStates.TEXT_ENTERED -> {
                        viewModel.sendMessage(SendMessageData(text = viewModel.currentState.text))
                        playMessageSound(requireContext())
                    }
                    InputStates.VOICE_RECORDED -> {
                        viewModel.sendMessage(SendMessageData(audio = viewModel.currentState.audioFile))
                        playMessageSound(requireContext())
                    }
                    InputStates.VOICE_RECORDING -> viewModel.stopRecording()
                }
            },
            onVoice = {
                requestMicrophonePermission()
            },
            onCamera = {
                viewModel.updateState { copy(inputState = InputStates.INITIAL) }
                requestCameraPermission()
            },
            onGallery = {
                viewModel.updateState { copy(inputState = InputStates.INITIAL) }
                requestReadPermission()
            }
        )
    }

    @ExperimentalMaterialApi
    @Composable
    private fun MessagesList(state: DialogUiState = testDialogUiState) {
        val pagingDataFlow = state.pagingDataFlow
        if (pagingDataFlow != null) {
            val lazyPagingItems = pagingDataFlow.collectAsLazyPagingItems()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                reverseLayout = true,
            ) {
                items(lazyPagingItems) { ShowMessage(it, state.playingState) }
            }
        }
    }

    @ExperimentalMaterialApi
    @Composable
    private fun ShowMessage(messageData: MessageViewData?, playingState: PlayingState?) {
        if (messageData != null)
            LaunchedEffect(messageData.message.id) {
                viewModel.notifyAsViewed(messageData)
            }

        if (messageData?.message?.image != null || messageData?.message?.video != null) {
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(messageData.message.id) {
                withContext(Dispatchers.IO) {
                    val image = if (messageData.message.image != null) messageData.message.image
                    else videoThumbnail(messageData.message.video!!)
                    bitmap = getBitmapFromGallery(image, requireContext(), 1)
                }
            }
            DisposableEffect(messageData.message.id) {
                onDispose {
                    bitmap = null
                    messageData.bitmap = null
                }
            }
            messageData.bitmap = bitmap
        }
        if (messageData != null) {
            messageData.playingState =
                if (playingState?.messageId == messageData.message.messageId) playingState else null
            messageData.replyPlayingState =
                if (playingState?.messageId == messageData.message.replyMessage?.messageId) playingState else null
        }
        MessageCompose(
            data = messageData,
            onClick = {
                when {
                    messageData?.message?.video != null -> videoOnClick(messageData.message)
                    messageData?.message?.image != null -> imageOnClick(messageData.message)
                }
            },
            onSwipe = {
                messageData?.message?.let {
                    viewModel.updateState { copy(replyMessage = it) }
                }
            },
            playOnClick = { audioFile: String?, messageId: String? ->
                if (playingState?.paused != false) {
                    viewModel.startPlaying(audioFile, messageId)
                } else {
                    viewModel.pausePlaying()
                }
            }
        )
    }

    private fun videoOnClick(message: Message) {
        if (message.received == 1 || message.isMine) {
            val uriWithSource = getUriWithSource(message.video!!, requireContext())
            if (uriWithSource.uri != null) {
                findNavController().navigate(
                    R.id.exoPlayerViewFragment,
                    Bundle().apply { putString("uri", uriWithSource.uri.toString()) })
            }
        }
    }

    private fun imageOnClick(message: Message) {
        if (message.received == 1 || message.isMine) {
            //val extras = FragmentNavigator.Extras.Builder()
            //    .addSharedElement(imageView, imageView.transitionName)
            //    .build()
            val bundle = Bundle().apply {
                putString("image", message.image)
                putInt("quality", 2)
                putBoolean("gallery", true)
            }
            findNavController().navigate(R.id.imageViewFragment, bundle, null)
        }
    }

    fun hideKeyBoard() {
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun navigateToUserFragment(userId: Long) {
        findNavController().navigate(
            R.id.userViewFragment,
            Bundle().apply { putLong("id", userId) })
    }

    private fun navigateToSendMediaFragment(uri: Uri? = null, path: String? = null) {
        val conversationId = arguments?.get("id") as Long
        findNavController().navigate(R.id.sendMediaFragment, Bundle().apply {
            if (path != null) putString("path", path)
            if (uri != null) putParcelable("uri", uri)
            putLong("conversationId", conversationId)
        })
    }

    private val fileChooser = registerForActivityResult(StartFileChooser()) { uri ->
        if (uri != null) navigateToSendMediaFragment(uri = uri)
    }

    private val takePicture = registerForActivityResult(TakePictureFromCamera()) {
        navigateToSendMediaFragment(path = viewModel.currentState.photoPath)
    }

    @SuppressLint("SimpleDateFormat")
    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date());
        val imageFileName = "JPEG_" + timeStamp + "_";
        val storageDir = requireContext().filesDir
        return File.createTempFile(imageFileName, ".jpg", storageDir).apply { deleteOnExit() }
    }

    private val readPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            fileChooser.launch(Unit)
        }

    fun requestReadPermission() {
        readPermission.launch(READ_EXTERNAL_STORAGE)
    }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            val photoFile = createTempImageFile()
            viewModel.updateState { copy(photoPath = photoFile.path) }
            takePicture.launch(photoFile)
        }

    fun requestCameraPermission() {
        cameraPermission.launch(CAMERA)
    }

    private val microphonePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.startRecording()
        }

    fun requestMicrophonePermission() {
        microphonePermission.launch(RECORD_AUDIO)
    }
}

private class StartFileChooser : ActivityResultContract<Unit, Uri>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
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