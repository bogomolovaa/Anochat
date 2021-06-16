package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentExoVideoViewBinding
import bogomolov.aa.anochat.databinding.FragmentImageViewBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlin.math.max

private const val KEY_WINDOW = "window"
private const val KEY_POSITION = "position"

class ExoPlayerViewFragment : Fragment(R.layout.fragment_exo_video_view) {
    private val binding by bindingDelegate(FragmentExoVideoViewBinding::bind)
    private var savedSystemUiVisibility = 0
    private var startWindow = 0
    private var startPosition = 0L
    private var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            startWindow = savedInstanceState.getInt(KEY_WINDOW)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as AppCompatActivity
        mainActivity.setSupportActionBar(binding.toolbar)
        val navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar?.title = ""

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

    private fun savePosition(){
        startWindow = player?.currentWindowIndex ?: 0
        startPosition = max(0, player?.contentPosition ?: 0L)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_WINDOW, startWindow)
        outState.putLong(KEY_POSITION, startPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.decorView.systemUiVisibility = savedSystemUiVisibility
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
            binding.playerView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer()
            binding.playerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        savePosition()
        if (Build.VERSION.SDK_INT <= 23) {
            binding.playerView.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            binding.playerView.onPause()
            releasePlayer()
        }
    }

    private fun getUri() = arguments?.getString("uri")?.toUri()

    private fun initializePlayer() {
        val mediaItem = MediaItem.fromUri(getUri()!!)
        player = SimpleExoPlayer.Builder(requireContext()).build().apply {
            binding.playerView.player = this
            setMediaItem(mediaItem)
            playWhenReady = true
            seekTo(startWindow, startPosition)
            prepare()
            play()
        }
        binding.playerView.hideController()
        binding.playerView.findViewById<View>(R.id.exo_settings).visibility = View.GONE
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }


}