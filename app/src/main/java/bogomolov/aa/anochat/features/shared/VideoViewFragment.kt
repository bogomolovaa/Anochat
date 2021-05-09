package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.databinding.FragmentVideoViewBinding

private const val PLAYBACK_TIME = "play_time"

class VideoViewFragment : Fragment() {
    private lateinit var binding: FragmentVideoViewBinding
    private var savedSystemUiVisibility = 0
    private var mCurrentPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) mCurrentPosition = savedInstanceState.getInt(PLAYBACK_TIME);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentVideoViewBinding.inflate(inflater, container, false).also { binding = it }.root

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as AppCompatActivity
        mainActivity.setSupportActionBar(binding.toolbar)
        val navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar?.title = ""
        setHasOptionsMenu(true)


        val controller = MediaController(requireContext())
        controller.setMediaPlayer(binding.videoView)
        binding.videoView.setMediaController(controller)

        requireActivity().window.decorView.systemUiVisibility = savedSystemUiVisibility
        requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PLAYBACK_TIME, binding.videoView.currentPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.decorView.systemUiVisibility = savedSystemUiVisibility
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            binding.videoView.pause()
        }
    }

    private fun getUri() = arguments?.getString("uri")?.toUri()

    private fun initializePlayer() {
        binding.videoView.setVideoURI(getUri())

        binding.videoView.setOnPreparedListener {
            if (mCurrentPosition > 0) {
                binding.videoView.seekTo(mCurrentPosition);
            } else {
                binding.videoView.seekTo(1);
            }
            binding.videoView.start()
        }

        binding.videoView.setOnCompletionListener {
            binding.videoView.seekTo(0)
        }
    }


    private fun releasePlayer() {
        binding.videoView.stopPlayback()
    }
}