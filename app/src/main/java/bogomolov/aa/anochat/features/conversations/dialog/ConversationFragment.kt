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
import android.media.MediaRecorder
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
import bogomolov.aa.anochat.repository.getRandomString
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

    private var currentFabIcon: Int? = null

    override fun updateView(newState: DialogUiState, currentState: DialogUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData) setPagedList(newState)
        if (newState.conversation != currentState.conversation) setConversation(newState.conversation!!)
        if (newState.onlineStatus != currentState.onlineStatus)
            binding.statusText.text = newState.onlineStatus

        if (newState.fabExpanded != currentState.fabExpanded) {
            if (newState.fabExpanded) {
                binding.fabFile.visibility = View.VISIBLE
                binding.fabMic.visibility = View.VISIBLE
                binding.fabCamera.visibility = View.VISIBLE
            } else {
                binding.fabFile.visibility = View.INVISIBLE
                binding.fabMic.visibility = View.INVISIBLE
                binding.fabCamera.visibility = View.INVISIBLE
            }
        }
        if (newState.replyMessage != currentState.replyMessage) {
            if (newState.replyMessage == null) {
                binding.replyLayout.visibility = View.INVISIBLE
                binding.replyText.text = ""
            } else {
                binding.replyLayout.visibility = View.VISIBLE
                binding.replyText.text = newState.replyMessage.text
                if (newState.replyMessage.image != null) {
                    val file = File(getFilesDir(requireContext()), newState.replyMessage.image)
                    if (file.exists()) {
                        binding.replyImage.setImageBitmap(BitmapFactory.decodeFile(file.path))
                        binding.replyImage.visibility = View.VISIBLE
                    }
                } else {
                    binding.replyImage.visibility = View.GONE
                }
                if (newState.replyMessage.audio != null) {
                    binding.replayAudio.setFile(newState.replyMessage.audio)
                    binding.replayAudio.visibility = View.VISIBLE
                } else {
                    binding.replayAudio.visibility = View.GONE
                }
            }
        }
        if (newState.text != currentState.text) binding.messageInputText.setText(newState.text)
        if (newState.recorder != currentState.recorder || newState.audioFile != currentState.audioFile) {
            if (newState.recorder != null) {
                if (newState.audioFile != null) {
                    binding.audioLayout.visibility = View.VISIBLE
                    binding.textLayout.visibility = View.GONE
                }
            } else {
                binding.audioLayout.visibility = View.GONE
                if (newState.audioFile != null) {
                    binding.playAudioInput.setFile(viewModel.state.audioFile!!)
                    binding.playAudioInput.visibility = View.VISIBLE
                }else{
                    binding.playAudioInput.visibility = View.GONE
                    binding.textLayout.visibility = View.VISIBLE
                }
            }
        }

        var fabIcon = R.drawable.plus_icon
        if (newState.fabExpanded) fabIcon = R.drawable.clear_icon
        if (newState.textEntered) fabIcon = R.drawable.send_icon
        if (newState.recorder != null) {
            if (newState.audioFile != null) fabIcon = R.drawable.stop_icon
        } else {
            if (newState.audioFile != null) fabIcon = R.drawable.send_icon
        }
        if (fabIcon != currentFabIcon) {
            binding.fab.setImageResource(fabIcon)
            currentFabIcon = fabIcon
        }
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
            Log.i("test", "pagedListLiveData UPDATED")
            (binding.recyclerView.adapter as MessagesPagedAdapter).submitList(pagedList)
            if (uiState.recyclerViewState != null) {
                binding.recyclerView.layoutManager?.onRestoreInstanceState(uiState.recyclerViewState)
            } else {
                viewModel.setStateAsync { copy(scrollEnd = true) }
                binding.recyclerView.scrollToPosition(pagedList.size - 1);
            }
            binding.recyclerView.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }
    }

    private fun setupUserInput(view: View) {
        setFabDefaultClickListener()
        binding.messageInputText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) viewModel.setStateAsync { copy(scrollEnd = true) }
        }
        binding.messageInputText.doOnTextChanged { textInput, _, _, _ ->
            hideFabs()
            val text = textInput.toString()
            if (!textInput.isNullOrEmpty()) {
                viewModel.setStateAsync { copy(text = text, scrollEnd = true, textEntered = true) }
            } else {
                viewModel.setStateAsync { copy(text = text.toString(), textEntered = false) }
            }
        }
        binding.fabMic.setOnClickListener {
            hideFabs()
            requestMicrophonePermission()
            viewModel.setStateAsync { copy(scrollEnd = false) }
        }
        binding.fabFile.setOnClickListener {
            hideFabs()
            requestReadPermission()
        }
        binding.fabCamera.setOnClickListener {
            hideFabs()
            requestCameraPermission()
        }
        emojiPopup = EmojiPopup.Builder.fromRootView(view).build(binding.messageInputText)
        binding.emojiIcon.setOnClickListener {
            emojiPopup.toggle()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            emojiPopup.dismiss()
            navController.navigateUp()
        }
        binding.playAudioInput.onClose {
            viewModel.setStateAsync {
                copy(
                    scrollEnd = false,
                    textEntered = false,
                    audioFile = null
                )
            }
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.recyclerView
        recyclerView.setItemViewCacheSize(20)
        val adapter = createRecyclerViewAdapter()
        recyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.addOnScrollListener(getScrollListener(linearLayoutManager, adapter))
        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (viewModel.state.scrollEnd) { // && (bottom < oldBottom && adapter.itemCount > 0)
                binding.recyclerView.postDelayed({
                    binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
                    viewModel.setStateAsync { copy(scrollEnd = false) }
                }, 100)
            }
        }
    }

    private fun createRecyclerViewAdapter(): MessagesPagedAdapter {
        val data = ActionModeData<MessageView>(R.menu.messages_menu, binding.toolbar)
        data.actionsMap[R.id.delete_messages_action] =
            { _, items ->
                viewModel.addAction(DeleteMessagesAction(items.map { it.message.id }.toSet()))
            }
        data.actionsMap[R.id.reply_message_action] = { _, items ->
            val message = items.iterator().next()
            onReply(message.message)
        }
        val adapter =
            MessagesPagedAdapter(
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

    private fun getScrollListener(
        linearLayoutManager: LinearLayoutManager,
        adapter: MessagesPagedAdapter
    ): RecyclerView.OnScrollListener {
        var loadImagesJob: Job? = null
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstId = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                val lastId = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                loadImagesJob?.cancel()
                loadImagesJob = lifecycleScope.launch {
                    delay(1000)
                    Log.i("test", "onScrolled [$firstId,$lastId]")
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

    private fun smoothScrollToLastPosition() {
        val lastPosition = binding.recyclerView.adapter?.itemCount ?: 0 - 1
        if (lastPosition > 0) binding.recyclerView.smoothScrollToPosition(lastPosition)
    }

    private fun onReply(message: Message) {
        viewModel.setStateAsync { copy(replyMessage = message) }
        smoothScrollToLastPosition()
        binding.removeReply.setOnClickListener {
            viewModel.setStateAsync { copy(replyMessage = null) }
        }
    }

    private fun setFabDefaultClickListener() {
        binding.fab.setOnClickListener {
            //viewModel.setStateAsync { copy(scrollEnd = false) }
            if (viewModel.state.textEntered) {
                viewModel.addAction(
                    SendMessageAction(
                        text = viewModel.state.text,
                        audio = viewModel.state.audioFile,
                        image = viewModel.state.photoPath
                    )
                )
            } else {
                if (viewModel.state.fabExpanded) {
                    hideFabs()
                } else {
                    expandFabs()
                }
            }
        }
    }

    private fun hideFabs() {
        binding.fabMic.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    viewModel.setStateAsync { copy(fabExpanded = false) }
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    viewModel.setStateAsync { copy(fabExpanded = false) }
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    viewModel.setStateAsync { copy(fabExpanded = false) }
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun expandFabs() {
        binding.fabMic.animate()
            .translationY(-600f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    viewModel.setStateAsync { copy(fabExpanded = true) }
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.animate()
            .translationY(-200f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    viewModel.setStateAsync { copy(fabExpanded = true) }
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.animate()
            .translationY(-400f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    viewModel.setStateAsync { copy(fabExpanded = true) }
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    override fun onPause() {
        super.onPause()
        emojiPopup.dismiss()
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FILE_CHOOSER_CODE -> {
                    val uri = intent?.data
                    if (uri != null) redirectToSendMediaFragment(uri = uri)
                }
                CAMERA_CODE -> redirectToSendMediaFragment(path = viewModel.state.photoPath)
            }
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
    private fun startRecording() {
        val audioFile = getRandomString(20) + ".3gp"
        val startTime = System.currentTimeMillis()
        val job = lifecycleScope.launch {
            while (true) {
                val time = System.currentTimeMillis() - startTime
                val timeString = SimpleDateFormat("mm:ss").format(Date(time))
                binding.audioLengthText.text = timeString
                delay(1000)
            }
        }
        binding.fab.setOnClickListener {
            job.cancel()
            stopRecording()
        }
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setOutputFile(File(getFilesDir(requireContext()), audioFile).path)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.prepare()
        recorder.start()
        viewModel.setStateAsync { copy(audioFile = audioFile, recorder = recorder) }
    }

    private fun stopRecording() {
        val recorder = viewModel.state.recorder
        if (recorder != null) {
            recorder.stop()
            recorder.release()
        }
        setFabDefaultClickListener()
        viewModel.setStateAsync { copy(textEntered = true, recorder = null) }
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
            Log.i("test", "photoUri $photoUri")
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
                MICROPHONE_PERMISSIONS_CODE -> startRecording()
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