package bogomolov.aa.anochat.features.conversations.dialog


import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import androidx.core.view.doOnPreDraw
import androidx.core.widget.doOnTextChanged
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.features.shared.ActionModeData
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import bogomolov.aa.anochat.repository.getFilesDir
import com.google.android.material.card.MaterialCardView
import com.vanniktech.emoji.EmojiPopup
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val TAG = "ConversationFragment"

class ConversationFragment : Fragment(), UpdatableView<DialogUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ConversationViewModel by navGraphViewModels(R.id.dialog_graph) { viewModelFactory }
    private lateinit var navController: NavController
    private lateinit var binding: FragmentConversationBinding
    private lateinit var emojiPopup: EmojiPopup
    private var recyclerViewRestored = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conversationId = arguments?.get("id") as Long
        viewModel.addAction(InitConversationAction(conversationId) {
            val locale = ConfigurationCompat.getLocales(requireContext().resources.configuration)[0]
            MessageView.toMessageViewsWithDateDelimiters(it, locale)
        })
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onStart() {
        super.onStart()
        postponeEnterTransition()
        recyclerViewRestored = false
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.state.inputState == InputStates.FAB_EXPAND)
            viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConversationBinding.inflate(inflater, container, false)
        (activity as MainActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        setupRecyclerView()
        setupUserInput(binding.root)

        return binding.root
    }

    override fun updateView(newState: DialogUiState, currentState: DialogUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData) setPagedList(newState)
        if (newState.conversation != currentState.conversation) setConversation(newState.conversation!!)
        if (newState.onlineStatus != currentState.onlineStatus)
            binding.statusText.text = newState.onlineStatus
        if (newState.audioLengthText != currentState.audioLengthText)
            binding.audioLengthText.text = newState.audioLengthText
        if (newState.replyMessage != currentState.replyMessage) setReplyMessage(newState.replyMessage)
        if (newState.inputState != currentState.inputState) setInputState(newState)
    }

    private fun setReplyMessage(replyMessage: Message?) {
        if (replyMessage == null) {
            binding.replyLayout.visibility = View.GONE
            binding.replyImage.visibility = View.INVISIBLE
            binding.replayAudio.visibility = View.INVISIBLE
        } else {
            binding.replyLayout.visibility = View.VISIBLE
            binding.replyText.text = replyMessage.text
            if (replyMessage.image != null) {
                val file = File(getFilesDir(requireContext()), replyMessage.image)
                if (file.exists()) {
                    binding.replyImage.setImageBitmap(BitmapFactory.decodeFile(file.path))
                    binding.replyImage.visibility = View.VISIBLE
                }
            }
            if (replyMessage.audio != null) {
                binding.replayAudio.setFile(replyMessage.audio)
                binding.replayAudio.visibility = View.VISIBLE
            }
        }
    }

    private fun setInputState(state: DialogUiState) {
        hideInput(state)
        if (state.inputState == InputStates.INITIAL) {
            binding.textLayout.visibility = View.VISIBLE
            binding.messageInputText.setText("")
            binding.fab.setImageResource(R.drawable.plus_icon)
        }
        if (state.inputState == InputStates.TEXT_ENTERED) {
            if (binding.messageInputText.text.toString() != state.text)
                binding.messageInputText.setText(state.text)
            binding.fab.setImageResource(R.drawable.send_icon)
        }
        if (state.inputState == InputStates.FAB_EXPAND) {
            binding.textLayout.visibility = View.VISIBLE
            binding.fabFile.visibility = View.VISIBLE
            binding.fabMic.visibility = View.VISIBLE
            binding.fabCamera.visibility = View.VISIBLE
            binding.fab.setImageResource(R.drawable.clear_icon)
        }
        if (state.inputState == InputStates.VOICE_RECORDING) {
            binding.audioLayout.visibility = View.VISIBLE
            binding.fab.setImageResource(R.drawable.stop_icon)
        }
        if (state.inputState == InputStates.VOICE_RECORDED) {
            binding.playAudioInput.visibility = View.VISIBLE
            binding.playAudioInput.setFile(viewModel.state.audioFile!!)
            binding.fab.setImageResource(R.drawable.send_icon)
        }
    }

    private fun hideInput(state: DialogUiState) {
        //fab
        binding.fabFile.visibility = View.INVISIBLE
        binding.fabMic.visibility = View.INVISIBLE
        binding.fabCamera.visibility = View.INVISIBLE
        //audio
        binding.playAudioInput.visibility = View.GONE
        binding.audioLayout.visibility = View.GONE
        //text
        if (state.inputState != InputStates.TEXT_ENTERED) binding.textLayout.visibility = View.GONE
    }

    private fun setConversation(conversation: Conversation) {
        if (conversation.user.photo != null) binding.userPhoto.setFile(conversation.user.photo)
        binding.usernameText.text = conversation.user.name
        binding.usernameLayout.setOnClickListener {
            navController.navigate(
                R.id.userViewFragment,
                Bundle().apply { putLong("id", conversation.user.id) })
        }
    }

    private fun setPagedList(uiState: DialogUiState) {
        uiState.pagedListLiveData!!.observe(viewLifecycleOwner) { pagedList ->
            (binding.recyclerView.adapter as MessagesPagedAdapter).submitList(pagedList)
            restoreRecyclerViewPosition()
            binding.recyclerView.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }
    }

    private fun restoreRecyclerViewPosition() {
        if (viewModel.state.recyclerViewState != null && !recyclerViewRestored) {
            binding.recyclerView.layoutManager?.onRestoreInstanceState(viewModel.state.recyclerViewState)
            recyclerViewRestored = true
        } else {
            scrollToEnd()
        }
    }

    private fun setupUserInput(view: View) {
        setFabClickListener()
        binding.messageInputText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) scrollToEnd()
        }
        binding.messageInputText.doOnTextChanged { textInput, _, _, _ ->
            val text = textInput.toString()
            if (viewModel.state.text != text) {
                if (text.isNotEmpty()) {
                    viewModel.setStateAsync {
                        copy(text = text, inputState = InputStates.TEXT_ENTERED)
                    }
                } else {
                    viewModel.setStateAsync { copy(text = "", inputState = InputStates.INITIAL) }
                }
            }
        }
        binding.fabMic.setOnClickListener {
            hideFabs()
            requestMicrophonePermission()
        }
        binding.fabFile.setOnClickListener {
            hideFabs { viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) } }
            requestReadPermission()
        }
        binding.fabCamera.setOnClickListener {
            hideFabs { viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) } }
            requestCameraPermission()
        }
        emojiPopup = EmojiPopup.Builder.fromRootView(view).build(binding.messageInputText)
        binding.emojiIcon.setOnClickListener { emojiPopup.toggle() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            hideKeyBoard()
            navController.navigateUp()
        }
        binding.playAudioInput.onClose {
            viewModel.setStateAsync { copy(inputState = InputStates.INITIAL, audioFile = null) }
        }
    }

    private fun setupRecyclerView() {
        with(binding.recyclerView) {
            setItemViewCacheSize(20)
            this.adapter = createRecyclerViewAdapter()
            layoutManager = LinearLayoutManager(context)
            addOnScrollListener(getScrollListener())
        }
    }

    private fun scrollToEnd() {
        val adapter = binding.recyclerView.adapter!!
        binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    private fun createRecyclerViewAdapter(): MessagesPagedAdapter {
        val data = ActionModeData<MessageView>(R.menu.messages_menu, binding.toolbar)
        data.actionsMap[R.id.delete_messages_action] = { _, items ->
            viewModel.addAction(DeleteMessagesAction(items.map { it.message.id }.toSet()))
        }
        data.actionsMap[R.id.reply_message_action] = { _, items ->
            val message = items.iterator().next()
            onReply(message.message)
        }
        val adapter = MessagesPagedAdapter(
            windowWidth = getWindowWidth(),
            onReply = ::onReply,
            actionModeData = data
        )
        adapter.setHasStableIds(true)
        return adapter
    }

    private fun getWindowWidth(): Int {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getScrollListener(): RecyclerView.OnScrollListener {
        val linearLayoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
        val adapter = binding.recyclerView.adapter as MessagesPagedAdapter
        var loadImagesJob: Job? = null
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy != 0) hideKeyBoard()
                val firstId = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                val lastId = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                loadImagesJob?.cancel()
                loadImagesJob = lifecycleScope.launch {
                    delay(1000)
                    for (id in firstId..lastId) if (id != -1) {
                        val viewHolder = recyclerView.findViewHolderForLayoutPosition(id)
                        if (viewHolder != null) adapter.loadDetailedImage(id, viewHolder)
                    }
                    saveRecyclerViewPosition()
                }
            }
        }
    }

    private fun saveRecyclerViewPosition() {
        viewModel.setStateAsync {
            copy(recyclerViewState = binding.recyclerView.layoutManager?.onSaveInstanceState())
        }
    }

    private fun onReply(message: Message) {
        viewModel.setStateAsync { copy(replyMessage = message) }
        binding.removeReply.setOnClickListener {
            viewModel.setStateAsync { copy(replyMessage = null) }
        }
    }

    private fun setFabClickListener() {
        binding.fab.setOnClickListener {
            when (viewModel.state.inputState) {
                InputStates.INITIAL -> expandFabs()
                InputStates.FAB_EXPAND -> hideFabs {
                    viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) }
                }
                InputStates.TEXT_ENTERED ->
                    viewModel.addAction(SendMessageAction(text = viewModel.state.text))
                InputStates.VOICE_RECORDED ->
                    viewModel.addAction(SendMessageAction(audio = viewModel.state.audioFile))
                InputStates.VOICE_RECORDING -> viewModel.addAction(StopRecordingAction())
            }
        }
    }

    private fun hideFabs(onAnimationEnd: () -> Unit = {}) {
        binding.fabMic.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    binding.fabMic.animate().setListener(null)
                    onAnimationEnd()
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.animate()
            .translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.animate()
            .translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun expandFabs() {
        viewModel.setStateAsync { copy(inputState = InputStates.FAB_EXPAND) }
        binding.fabMic.animate()
            .translationY(-600f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.animate()
            .translationY(-200f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.animate()
            .translationY(-400f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    override fun onPause() {
        super.onPause()
        hideKeyBoard()
    }

    private fun hideKeyBoard() {
        emojiPopup.dismiss()
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == RESULT_OK)
            when (requestCode) {
                FILE_CHOOSER_CODE -> {
                    val uri = intent?.data
                    if (uri != null) redirectToSendMediaFragment(uri = uri)
                }
                CAMERA_CODE -> redirectToSendMediaFragment(path = viewModel.state.photoPath)
            }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun redirectToSendMediaFragment(uri: Uri? = null, path: String? = null) {
        navController.navigate(R.id.sendMediaFragment, Bundle().apply {
            if (path != null) putString("path", path)
            if (uri != null) putParcelable("uri", uri)
            putLong("conversationId", viewModel.state.conversation!!.id)
        })
    }

    private fun startFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_file)),
                FILE_CHOOSER_CODE
            )
        } catch (ex: ActivityNotFoundException) {
            Log.w(TAG, "File manager not installed")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date());
        val imageFileName = "JPEG_" + timeStamp + "_";
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir).apply { deleteOnExit() }
    }

    private fun takePictureFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile = createTempImageFile()
            viewModel.setStateAsync { copy(photoPath = photoFile.path) }
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "bogomolov.aa.anochat.fileprovider",
                photoFile
            )
            Log.d(TAG, "photoUri $photoUri")
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(takePictureIntent, CAMERA_CODE)
        }
    }

    private fun requestReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(READ_PERMISSION), READ_PERMISSIONS_CODE)
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSIONS_CODE)
    }

    private fun requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(MICROPHONE_PERMISSION), MICROPHONE_PERMISSIONS_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PERMISSION_GRANTED)
            when (requestCode) {
                READ_PERMISSIONS_CODE -> startFileChooser()
                CAMERA_PERMISSIONS_CODE -> takePictureFromCamera()
                MICROPHONE_PERMISSIONS_CODE ->
                    viewModel.addAction(StartRecordingAction(requireContext()))
            }
    }

    companion object {
        private const val FILE_CHOOSER_CODE: Int = 0
        private const val CAMERA_CODE: Int = 1
        private const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val MICROPHONE_PERMISSION = Manifest.permission.RECORD_AUDIO
        private const val READ_PERMISSIONS_CODE = 1001
        private const val CAMERA_PERMISSIONS_CODE = 1002
        private const val MICROPHONE_PERMISSIONS_CODE = 1003
    }
}

@BindingAdapter(value = ["android:layout_marginLeft", "android:layout_marginRight"])
fun setLayoutMargin(view: MaterialCardView, marginLeft: Float, marginRight: Float) {
    val p = view.layoutParams as ViewGroup.MarginLayoutParams
    p.setMargins(marginLeft.toInt(), p.topMargin, marginRight.toInt(), p.bottomMargin);
    view.requestLayout()
}