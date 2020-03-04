package bogomolov.aa.anochat.view.fragments


import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
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
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class ConversationFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ConversationViewModel by activityViewModels { viewModelFactory }

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
        mainActivity.setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        viewModel.conversationLiveData.observe(viewLifecycleOwner) {
            mainActivity.supportActionBar!!.title = it.user.name
        }

        val conversationId = arguments?.get("id") as Long
        mainActivity.conversationId = conversationId
        val adapter = MessagesPagedAdapter(context = requireContext())
        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.loadMessages(conversationId).observe(viewLifecycleOwner) {
            Log.i("test", "pagedListLiveData observed from conversationId ${conversationId}")
            adapter.submitList(it)
            binding.recyclerView.scrollToPosition(it.size - 1);
        }
        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom && adapter.itemCount > 0) {
                binding.recyclerView.postDelayed({
                    binding.recyclerView.smoothScrollToPosition(adapter.itemCount - 1);
                }, 100)
            }
        }

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

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Log.i("test", "onActivityResult $resultCode $intent requestCode $requestCode")
        if (resultCode == RESULT_OK && intent != null)
            when (requestCode) {
                FILE_CHOOSER_CODE -> {
                    val uri = intent.data
                    if (uri != null) {
                        Log.i("test", "File Uri: $uri")
                        val path = getPath(requireContext(), uri)
                        Log.i("test", "File Path: $path")
                    }
                }
                CAMERA_CODE -> {
                    val bitmap = intent.extras?.get("data") as Bitmap

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

    private fun takePictureFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_CODE);
    }

    private fun requestReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(READ_PERMISSION), READ_PERMISSIONS_CODE)
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(READ_PERMISSION), READ_PERMISSIONS_CODE)
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
        private const val READ_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val READ_PERMISSIONS_CODE = 1001
        private const val CAMERA_PERMISSIONS_CODE = 1002
    }


}
