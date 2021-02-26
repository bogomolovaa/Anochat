package bogomolov.aa.anochat.features.conversations.dialog


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import androidx.navigation.ui.NavigationUI
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionInflater
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import com.google.android.material.card.MaterialCardView
import com.vanniktech.emoji.EmojiPopup
import dagger.android.support.AndroidSupportInjection
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val TAG = "ConversationFragment"

class ConversationFragment : Fragment(), RequestPermission {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ConversationViewModel by navGraphViewModels(R.id.dialog_graph) { viewModelFactory }
    private lateinit var navController: NavController
    private lateinit var emojiPopup: EmojiPopup
    private lateinit var updatableView: ConversationUpdatableView
    private lateinit var recyclerViewSetup: ConversationRecyclerViewSetup
    private lateinit var conversationInputSetup: ConversationInputSetup

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conversationId = arguments?.get("id") as Long
        viewModel.addAction(InitConversationAction(conversationId) {
            val locale = ConfigurationCompat.getLocales(requireContext().resources.configuration)[0]
            toMessageViewsWithDateDelimiters(it, locale)
        })
        recyclerViewSetup = ConversationRecyclerViewSetup(this, viewModel)
        updatableView = ConversationUpdatableView(this, recyclerViewSetup)
        conversationInputSetup = ConversationInputSetup(this, viewModel, recyclerViewSetup)
        lifecycle.addObserver(StateLifecycleObserver(updatableView, viewModel))

        enterTransition = Slide(Gravity.END).apply { duration = 375 }
        exitTransition = Fade().apply { duration = 375 }
        requireActivity().window.decorView.setBackgroundResource(R.color.conversation_background)
    }

    override fun onStart() {
        super.onStart()
        postponeEnterTransition()
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.state.inputState == InputStates.FAB_EXPAND)
            viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) }
    }

    override fun onPause() {
        super.onPause()
        hideKeyBoard()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentConversationBinding.inflate(inflater, container, false)
        (activity as MainActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        emojiPopup = EmojiPopup.Builder.fromRootView(binding.root).build(binding.messageInputText)
        binding.emojiIcon.setOnClickListener { emojiPopup.toggle() }

        updatableView.binding = binding
        conversationInputSetup.setup(binding)
        recyclerViewSetup.setup(binding) {
            startPostponedEnterTransition()
        }

        return binding.root
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

    fun navigateToUserFragment(userId: Long) {
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
    p.setMargins(marginLeft.toInt(), p.topMargin, marginRight.toInt(), p.bottomMargin)
    //view.requestLayout()
}

@BindingAdapter(value = ["android:layout_marginRight"])
fun setLayoutMargin(view: TextView, marginRight: Float) {
    val p = view.layoutParams as ViewGroup.MarginLayoutParams
    p.setMargins(p.leftMargin, p.topMargin, marginRight.toInt(), p.bottomMargin)
    //view.requestLayout()
}