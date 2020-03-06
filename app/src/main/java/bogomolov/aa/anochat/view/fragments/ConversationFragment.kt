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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.view.*
import androidx.core.widget.doOnTextChanged
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getPath
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.view.MainActivity
import bogomolov.aa.anochat.view.MessagesPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationViewModel
import com.google.android.material.card.MaterialCardView
import com.vanniktech.emoji.EmojiPopup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_conversations_list.*
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


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentConversationBinding>(
            inflater,
            R.layout.fragment_conversation,
            container,
            false
        )
        val mainActivity = activity as MainActivity
        val view = binding.root
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        mainActivity.setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar!!.title = ""

        conversationId = arguments?.get("id") as Long
        mainActivity.conversationId = conversationId
        val recyclerView = binding.recyclerView
        val adapter = MessagesPagedAdapter(activity = requireActivity()) {
            viewModel.recyclerViewState =
                recyclerView.layoutManager?.onSaveInstanceState()
        }
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


        var fabExpanded = false
        var textEntered = false
        val sendMessageAction = {
            val text = binding.messageInputText.text
            if (!text.isNullOrEmpty()) {
                Log.i("test", "message text: $text")
                viewModel.sendMessage(text.toString())
                binding.messageInputText.setText("")
            }
        }
        var hideFabs = {
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
        var expandFabs = {
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

        binding.fabFile.setOnClickListener {
            requestReadPermission()
        }
        binding.fabCamera.setOnClickListener {
            requestCameraPermission()
        }


        emojiPopup = EmojiPopup.Builder.fromRootView(view).build(binding.messageInputText)
        binding.emojiIcon.setOnClickListener {
            emojiPopup.toggle()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            emojiPopup.dismiss()
        }
        return view
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
        }
    }

    companion object {
        const val FILE_CHOOSER_CODE: Int = 0
        const val CAMERA_CODE: Int = 1
        private const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val READ_PERMISSIONS_CODE = 1001
        private const val CAMERA_PERMISSIONS_CODE = 1002
    }


}

@BindingAdapter(value = ["android:layout_marginLeft", "android:layout_marginRight"])
fun setLayoutMargin(view: MaterialCardView, marginLeft: Float, marginRight: Float) {
    val p = view.layoutParams as ViewGroup.MarginLayoutParams
    p.setMargins(marginLeft.toInt(), p.topMargin, marginRight.toInt(), p.bottomMargin);
    view.requestLayout();
}