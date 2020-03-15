package bogomolov.aa.anochat.view.fragments


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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.android.getPath
import bogomolov.aa.anochat.android.getRandomString
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.view.MainActivity
import bogomolov.aa.anochat.view.adapters.MessagesPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationViewModel
import com.google.android.material.card.MaterialCardView
import com.vanniktech.emoji.EmojiPopup
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class ConversationFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ConversationViewModel by activityViewModels { viewModelFactory }
    lateinit var navController: NavController
    var conversationId = 0L
    private var photoPath: String? = null
    private lateinit var emojiPopup: EmojiPopup
    private var replyMessageId: String? = null
    private var recorder: MediaRecorder? = null
    private lateinit var binding: FragmentConversationBinding
    private var audioFileName: String? = null


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<FragmentConversationBinding>(
            inflater,
            R.layout.fragment_conversation,
            container,
            false
        )
        val mainActivity = activity as MainActivity
        val view = binding.root
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        mainActivity.setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar!!.title = ""

        conversationId = arguments?.get("id") as Long
        mainActivity.conversationId = conversationId
        val recyclerView = binding.recyclerView
        recyclerView.setItemViewCacheSize(20);
        val adapter =
            MessagesPagedAdapter(activity = requireActivity(),
                onReply = {
                    binding.replyImage.visibility = View.INVISIBLE
                    binding.replayAudio.visibility = View.INVISIBLE
                    binding.replyText.text = it.text
                    replyMessageId = it.messageId
                    Log.i("test", "onReply replyMessageId $replyMessageId")
                    val lastPosition = recyclerView.adapter?.itemCount ?: 0 - 1
                    Log.i("test", "lastPosition $lastPosition")
                    if (lastPosition > 0) recyclerView.smoothScrollToPosition(lastPosition)
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
                },
                setRecyclerViewState = {
                    viewModel.recyclerViewState =
                        recyclerView.layoutManager?.onSaveInstanceState()
                })
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.loadMessages(conversationId).observe(viewLifecycleOwner) {
            Log.i("test", "pagedListLiveData observed from conversationId ${conversationId}")
            adapter.submitList(it)

            if (viewModel.recyclerViewState != null) {
                Log.i("test", "onRestoreInstanceState")
                recyclerView.layoutManager?.onRestoreInstanceState(viewModel.recyclerViewState)
                viewModel.recyclerViewState = null
            } else {
                binding.recyclerView.scrollToPosition(it.size - 1);
            }
            recyclerView.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom && adapter.itemCount > 0) {
                binding.recyclerView.postDelayed({
                    Log.i("test", "addOnLayoutChangeListener scrollToPosition")
                    binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
                }, 100)
            }
        }


        postponeEnterTransition()

        setFabDefaultOnClickListener()
        binding.messageInputText.doOnTextChanged { text, start, count, after ->
            hideFabs()
            if (!text.isNullOrEmpty()) {
                binding.fab.setImageResource(R.drawable.send_icon)
                textEntered = true
            } else {
                binding.fab.setImageResource(R.drawable.plus_icon)
                textEntered = false
            }
        }

        binding.fabMic.setOnClickListener {
            hideFabs()
            requestMicrophonePermission()
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
            if (audioFileName != null) {
                File(getFilesDir(requireContext()), audioFileName!!).delete()
                audioFileName = null
            }
        }

        viewModel.conversationLiveData.observe(viewLifecycleOwner) { user ->
            binding.usernameLayout.setOnClickListener {
                navController.navigate(
                    R.id.userViewFragment,
                    Bundle().apply { putLong("id", user.id) })
            }
        }



        return view
    }


    private var fabExpanded = false
    private var textEntered = false

    private fun sendMessageAction() {
        val text = binding.messageInputText.text
        if (audioFileName != null) {
            Log.i("test", "message audio: $audioFileName")
            viewModel.sendMessage(text.toString(), replyMessageId, audioFileName)
            binding.playAudioInput.visibility = View.GONE
            binding.textLayout.visibility = View.VISIBLE
        } else if (!text.isNullOrEmpty()) {
            Log.i("test", "message text: $text")
            viewModel.sendMessage(text.toString(), replyMessageId, null)
            binding.messageInputText.setText("")
            removeReply(binding)
        }
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
        replyMessageId = null
    }

    override fun onPause() {
        super.onPause()
        emojiPopup.dismiss()
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun redirectToSendMediaFragment(uri: Uri? = null, path: String? = null) {
        val path0 = path ?: getPath(requireContext(), uri!!)
        Log.i("test", "File Path: $path0")
        navController.navigate(R.id.sendMediaFragment, Bundle().apply {
            putString("path", path0)
            putLong("conversationId", conversationId)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Log.i("test", "onActivityResult $resultCode $intent requestCode $requestCode")
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
        audioFileName = getRandomString(20) + ".3gp"
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
        recorder.setOutputFile(File(getFilesDir(requireContext()), audioFileName!!).path)
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
        binding.playAudioInput.setFile(audioFileName!!)
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