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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.core.widget.doOnTextChanged
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
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
import bogomolov.aa.anochat.databinding.MessageLayoutBinding
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.features.conversations.dialog.actions.DeleteMessagesAction
import bogomolov.aa.anochat.features.conversations.dialog.actions.InitConversationAction
import bogomolov.aa.anochat.features.conversations.dialog.actions.SendMessageAction
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.features.shared.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.UpdatableView
import bogomolov.aa.anochat.repository.getFilesDir
import bogomolov.aa.anochat.repository.getRandomString
import bogomolov.aa.anochat.view.adapters.AdapterHelper
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
import kotlin.collections.HashMap


class ConversationFragment : Fragment(), UpdatableView<DialogUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ConversationViewModel by navGraphViewModels(R.id.dialog_graph) { viewModelFactory }
    private lateinit var navController: NavController
    private lateinit var binding: FragmentConversationBinding

    private var photoPath: String? = null
    private lateinit var emojiPopup: EmojiPopup
    private var replyId: String? = null
    private var recorder: MediaRecorder? = null
    private var audioFile: String? = null
    private var scrollEnd = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conversationId = arguments?.get("id") as Long
        viewModel.addAction(InitConversationAction(conversationId))
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
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_conversation,
            container,
            false
        )
        val mainActivity = activity as MainActivity
        val view = binding.root
        binding.lifecycleOwner = viewLifecycleOwner
        mainActivity.setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        setupRecyclerView()
        setupUserInput(view)

        return view
    }

    override fun updateView(newState: DialogUiState, currentState: DialogUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData) setPagedList(newState)
        if (newState.conversation != currentState.conversation) setConversation(newState.conversation!!)
        if (newState.onlineStatus != currentState.onlineStatus) binding.statusText.text =
            newState.onlineStatus
    }

    private fun setConversation(conversation: Conversation) {
        if (conversation.user.photo != null) binding.userPhoto.setFile(conversation.user.photo!!)
        binding.usernameText.text = conversation.user.name
        binding.usernameLayout.setOnClickListener {
            navController.navigate(
                R.id.userViewFragment,
                Bundle().apply { putLong("id", conversation.user.id) })
        }
    }

    private fun setPagedList(uiState: DialogUiState) {
        uiState.pagedListLiveData!!.observe(viewLifecycleOwner) {
            Log.i("test", "pagedListLiveData UPDATED")
            (binding.recyclerView.adapter as MessagesPagedAdapter).submitList(it)
            if (uiState.recyclerViewState != null) {
                binding.recyclerView.layoutManager?.onRestoreInstanceState(uiState.recyclerViewState)
                viewModel.setStateAsync { copy(recyclerViewState = null) }
            } else {
                scrollEnd = true
                binding.recyclerView.scrollToPosition(it.size - 1);
            }
            binding.recyclerView.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

    }

    private fun setupUserInput(view: View) {
        setFabDefaultOnClickListener()
        binding.messageInputText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                scrollEnd = true
            }
        }
        binding.messageInputText.doOnTextChanged { text, start, count, after ->
            hideFabs()
            if (!text.isNullOrEmpty()) {
                binding.fab.setImageResource(R.drawable.send_icon)
                textEntered = true
                scrollEnd = true
            } else {
                binding.fab.setImageResource(R.drawable.plus_icon)
                textEntered = false
            }
        }

        binding.fabMic.setOnClickListener {
            hideFabs()
            requestMicrophonePermission()
            scrollEnd = false
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
        binding.playAudioInput.setOnClose {
            binding.fab.setImageResource(R.drawable.plus_icon)
            textEntered = false
            binding.playAudioInput.visibility = View.GONE
            binding.textLayout.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.recyclerView
        recyclerView.setItemViewCacheSize(20)

        val actionsMap = HashMap<Int, (Set<Long>, Set<MessageView>) -> Unit>()
        actionsMap[R.id.delete_messages_action] =
            { ids, items -> viewModel.addAction(DeleteMessagesAction(ids)) }
        actionsMap[R.id.reply_message_action] = { ids, items ->
            val message = items.iterator().next()
            onReply(message.message)
        }
        val adapter =
            MessagesPagedAdapter(
                activity = requireActivity(),
                onReply = this::onReply,
                setRecyclerViewState = {
                    viewModel.setStateAsync {
                        copy(recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState())
                    }
                },
                helper = AdapterHelper(
                    menuId = R.menu.messages_menu,
                    actionsMap = actionsMap,
                    toolbar = binding.toolbar
                )
            )
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager
        var loadImagesJob: Job? = null
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstId = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                val lastId = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                loadImagesJob?.cancel()
                loadImagesJob = lifecycleScope.launch {
                    delay(1000)
                    for (id in firstId..lastId) if (id != -1) {
                        val vh =
                            recyclerView.findViewHolderForLayoutPosition(id) as AdapterHelper<MessageView, MessageLayoutBinding>.VH
                        adapter.itemShowed(id, vh.binding)
                    }
                }
            }
        })

        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (scrollEnd) { // && (bottom < oldBottom && adapter.itemCount > 0)
                binding.recyclerView.postDelayed({
                    binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
                    scrollEnd = false
                }, 100)
            }
        }
    }


    private fun onReply(it: Message) {
        binding.replyImage.visibility = View.GONE
        binding.replayAudio.visibility = View.GONE
        binding.replyText.text = it.text
        replyId = it.messageId
        val lastPosition = binding.recyclerView.adapter?.itemCount ?: 0 - 1
        if (lastPosition > 0) binding.recyclerView.smoothScrollToPosition(lastPosition)
        if (it.image != null) {
            val file = File(getFilesDir(requireContext()), it.image)
            if (file.exists()) {
                binding.replyImage.setImageBitmap(BitmapFactory.decodeFile(file.path))
                binding.replyImage.visibility = View.VISIBLE
            }
        }
        if (it.audio != null) {
            binding.replayAudio.setFile(it.audio)
            binding.replayAudio.visibility = View.VISIBLE
        }
        binding.removeReply.setOnClickListener {
            removeReply(binding)
        }
        binding.replyLayout.visibility = View.VISIBLE
    }


    private var fabExpanded = false
    private var textEntered = false

    private fun sendMessageAction() {
        val text = binding.messageInputText.text.toString()
        if (audioFile != null) {
            Log.i("test", "message audio: $audioFile")
            binding.playAudioInput.visibility = View.GONE
            binding.textLayout.visibility = View.VISIBLE
            audioFile = null
        } else if (text.isNotEmpty()) {
            Log.i("test", "message text: $text")
            binding.messageInputText.setText("")
            removeReply(binding)
        }
        viewModel.addAction(SendMessageAction(text = text, replyId = replyId, audio = audioFile))
    }

    private fun hideFabs() {
        binding.fabMic.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    binding.fabMic.visibility = View.INVISIBLE
                    fabExpanded = false
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    binding.fabFile.visibility = View.INVISIBLE
                    fabExpanded = false

                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    binding.fabCamera.visibility = View.INVISIBLE
                    fabExpanded = false
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fab.setImageResource(R.drawable.plus_icon)
    }

    private fun expandFabs() {
        binding.fabMic.visibility = View.VISIBLE
        binding.fabMic.animate()
            .translationY(-600f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    fabExpanded = true
                    binding.fab.setImageResource(R.drawable.clear_icon)
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.visibility = View.VISIBLE
        binding.fabFile.animate()
            .translationY(-200f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    fabExpanded = true
                    binding.fab.setImageResource(R.drawable.clear_icon)
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.visibility = View.VISIBLE
        binding.fabCamera.animate()
            .translationY(-400f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    fabExpanded = true
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun setFabDefaultOnClickListener() {
        binding.fab.setOnClickListener {
            scrollEnd = false
            if (textEntered) {
                sendMessageAction()
                textEntered = false
            } else {
                if (!fabExpanded) {
                    expandFabs()
                } else {
                    hideFabs()
                }
            }
        }
    }

    private fun removeReply(binding: FragmentConversationBinding) {
        binding.replyLayout.visibility = View.INVISIBLE
        replyId = null
    }

    override fun onPause() {
        super.onPause()
        emojiPopup.dismiss()
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun redirectToSendMediaFragment(uri: Uri? = null, path: String? = null) {
        navController.navigate(R.id.sendMediaFragment, Bundle().apply {
            if (path != null) putString("path", path)
            if (uri != null) putParcelable("uri", uri)
            putLong("conversationId", viewModel.currentState.conversation!!.id)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        //Log.i("test", "onActivityResult $resultCode $intent requestCode $requestCode")
        if (resultCode == RESULT_OK) {
            var uri: Uri? = null
            when (requestCode) {
                FILE_CHOOSER_CODE -> {
                    if (intent != null) {
                        uri = intent.data
                        if (uri != null)
                            redirectToSendMediaFragment(uri = uri)
                    }
                }
                CAMERA_CODE -> {
                    redirectToSendMediaFragment(path = photoPath)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
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
            Log.w("ConversationFragment", "File manager not installed")
        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun startRecording() {
        audioFile = getRandomString(20) + ".3gp"
        binding.textLayout.visibility = View.GONE
        binding.audioLayout.visibility = View.VISIBLE
        val startTime = System.currentTimeMillis()
        val job = lifecycleScope.launch {
            while (true) {
                val time = System.currentTimeMillis() - startTime
                val timeString = SimpleDateFormat("mm:ss").format(Date(time))
                binding.audioLengthText.text = timeString
                delay(1000)
            }
        }


        binding.fab.setImageResource(R.drawable.stop_icon)
        binding.fab.setOnClickListener {
            job.cancel()
            stopRecording()
        }
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setOutputFile(File(getFilesDir(requireContext()), audioFile!!).path)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.prepare()
        recorder.start()
        this.recorder = recorder
    }

    private fun stopRecording() {
        binding.audioLayout.visibility = View.GONE
        if (recorder != null) {
            recorder!!.stop()
            recorder!!.release()
        }
        recorder = null
        binding.fab.setImageResource(R.drawable.plus_icon)
        setFabDefaultOnClickListener()
        binding.playAudioInput.setFile(audioFile!!)
        binding.playAudioInput.visibility = View.VISIBLE
        binding.fab.setImageResource(R.drawable.send_icon)
        textEntered = true
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
            photoPath = photoFile.path
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
        when (requestCode) {
            READ_PERMISSIONS_CODE -> {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    startFileChooser()
                } else {
                    Log.i("test", "read perm not granted")
                }
            }
            CAMERA_PERMISSIONS_CODE -> {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    takePictureFromCamera()
                } else {
                    Log.i("test", "camera perm not granted")
                }
            }
            MICROPHONE_PERMISSIONS_CODE -> {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    startRecording()
                } else {
                    Log.i("test", "camera perm not granted")
                }
            }
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