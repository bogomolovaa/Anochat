package bogomolov.aa.anochat.features.conversations.dialog

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.transition.Fade
import androidx.transition.Slide
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.features.shared.bindingDelegate
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ConversationFragment"

@AndroidEntryPoint
class ConversationFragment : Fragment(R.layout.fragment_conversation) {
    val viewModel: ConversationViewModel by hiltNavGraphViewModels(R.id.dialog_graph)
    private var emojiPopup: EmojiPopup? = null
    val binding by bindingDelegate(FragmentConversationBinding::bind)
    private var stateLifecycleObserver: StateLifecycleObserver<DialogUiState>? = null
    var recyclerViewSetup: ConversationRecyclerViewSetup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val animationDuration = resources.getInteger(R.integer.animation_duration).toLong()
        enterTransition = Slide(Gravity.END).apply { duration = animationDuration }
        exitTransition = Fade().apply { duration = animationDuration }
        requireActivity().window.decorView.setBackgroundResource(R.color.conversation_background)
    }

    override fun onPause() {
        super.onPause()
        hideKeyBoard()
        if (viewModel.currentState.inputState == InputStates.FAB_EXPAND)
            viewModel.updateState { copy(inputState = InputStates.INITIAL) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val conversationId = arguments?.get("id") as Long
        recyclerViewSetup = ConversationRecyclerViewSetup(this, viewModel)
        val updatableView = ConversationUpdatableView(this)
        val conversationInputSetup = ConversationInputSetup(this, viewModel)
        stateLifecycleObserver = StateLifecycleObserver(updatableView, viewModel)
        viewLifecycleOwner.lifecycle.addObserver(stateLifecycleObserver!!)
        viewModel.initConversation(conversationId)

        binding.toolbar.setupWithNavController(findNavController())
        emojiPopup = EmojiPopup.Builder.fromRootView(binding.root).build(binding.messageInputText)
        binding.emojiIcon.setOnClickListener { emojiPopup?.toggle() }

        postponeEnterTransition()
        conversationInputSetup.setup()
        recyclerViewSetup?.setup {
            startPostponedEnterTransition()
        }

        binding.usernameLayout.setOnClickListener {
            val userId = viewModel.currentState.conversation?.user?.id
            if (userId != null) navigateToUserFragment(userId)
        }

        val uri = arguments?.getString("uri")?.toUri()
        if (uri != null) {
            arguments?.remove("uri")
            navigateToSendMediaFragment(uri = uri)
            return
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emojiPopup = null
        viewLifecycleOwner.lifecycle.removeObserver(stateLifecycleObserver!!)
        stateLifecycleObserver = null
        recyclerViewSetup = null
    }

    fun hideKeyBoard() {
        emojiPopup?.dismiss()
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        binding.messageInputText.clearFocus()
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