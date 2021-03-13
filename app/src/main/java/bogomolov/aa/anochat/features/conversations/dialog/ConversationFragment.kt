package bogomolov.aa.anochat.features.conversations.dialog

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.transition.Fade
import androidx.transition.Slide
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import com.google.android.material.card.MaterialCardView
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ConversationFragment"

@AndroidEntryPoint
class ConversationFragment : Fragment(), RequestPermission {
    val viewModel: ConversationViewModel by hiltNavGraphViewModels(R.id.dialog_graph)
    private lateinit var navController: NavController
    private lateinit var emojiPopup: EmojiPopup
    private lateinit var binding: FragmentConversationBinding

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
        if (viewModel.state.inputState == InputStates.FAB_EXPAND)
            viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentConversationBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerViewSetup = ConversationRecyclerViewSetup(this, viewModel)
        val updatableView = ConversationUpdatableView(recyclerViewSetup)
        val conversationInputSetup = ConversationInputSetup(this, viewModel, recyclerViewSetup)
        viewLifecycleOwner.lifecycle.addObserver(StateLifecycleObserver(updatableView, viewModel))
        val conversationId = arguments?.get("id") as Long
        viewModel.addAction(InitConversationAction(conversationId) {
            val locale = ConfigurationCompat.getLocales(requireContext().resources.configuration)[0]
            toMessageViewsWithDateDelimiters(it, locale)
        })

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        emojiPopup = EmojiPopup.Builder.fromRootView(binding.root).build(binding.messageInputText)
        binding.emojiIcon.setOnClickListener { emojiPopup.toggle() }

        postponeEnterTransition()
        updatableView.binding = binding
        conversationInputSetup.setup(binding)
        recyclerViewSetup.setup(binding) {
            startPostponedEnterTransition()
        }

        binding.usernameLayout.setOnClickListener {
            val userId = viewModel.state.conversation?.user?.id
            if (userId != null) navigateToUserFragment(userId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == RESULT_OK)
            when (requestCode) {
                FILE_CHOOSER_CODE -> {
                    val uri = intent?.data
                    if (uri != null) navigateToSendMediaFragment(uri = uri)
                }
                CAMERA_CODE -> navigateToSendMediaFragment(path = viewModel.state.photoPath)
            }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    fun hideKeyBoard() {
        emojiPopup.dismiss()
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun navigateToUserFragment(userId: Long) {
        navController.navigate(R.id.userViewFragment, Bundle().apply { putLong("id", userId) })
    }

    private fun navigateToSendMediaFragment(uri: Uri? = null, path: String? = null) {
        navController.navigate(R.id.sendMediaFragment, Bundle().apply {
            if (path != null) putString("path", path)
            if (uri != null) putParcelable("uri", uri)
            putLong("conversationId", viewModel.state.conversation!!.id)
        })
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
                    viewModel.addAction(StartRecordingAction())
            }
    }

    private fun startFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_file)),
                FILE_CHOOSER_CODE
            )
        } else {
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
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
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
        } else {
            Log.w(TAG, "Camera not available")
        }
    }

    override fun requestReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(READ_PERMISSION), READ_PERMISSIONS_CODE)
    }

    override fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSIONS_CODE)
    }

    override fun requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(MICROPHONE_PERMISSION), MICROPHONE_PERMISSIONS_CODE)
    }

    companion object {
        const val FILE_CHOOSER_CODE: Int = 0
        const val CAMERA_CODE: Int = 1
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
    p.setMargins(marginLeft.toInt(), p.topMargin, marginRight.toInt(), p.bottomMargin)
}

@BindingAdapter(value = ["android:layout_marginRight"])
fun setLayoutMargin(view: TextView, marginRight: Float) {
    val p = view.layoutParams as ViewGroup.MarginLayoutParams
    p.setMargins(p.leftMargin, p.topMargin, marginRight.toInt(), p.bottomMargin)
}