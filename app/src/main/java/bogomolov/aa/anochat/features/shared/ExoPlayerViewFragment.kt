package bogomolov.aa.anochat.features.shared

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView

class ExoPlayerViewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                val uri = arguments?.getString("uri")?.toUri()
                VideoView(uri!!)
            }
        }
}

@Composable
fun VideoView(uri: Uri) {
    val context = LocalContext.current
    val window = remember { mutableStateOf(0) }
    val position = remember { mutableStateOf(0L) }
    val autoPlay = remember { mutableStateOf(true) }
    val player = remember {
        val mediaItem = MediaItem.fromUri(uri)

        SimpleExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            playWhenReady = true
            seekTo(window.value, position.value)
            prepare()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun updateState() {
        autoPlay.value = player.playWhenReady
        window.value = player.currentWindowIndex
        position.value = 0L.coerceAtLeast(player.contentPosition)
    }

    val playerView = remember {
        val playerView = StyledPlayerView(context).apply {
            setShowFastForwardButton(false)
            setShowNextButton(false)
            setShowPreviousButton(false)
            setShowRewindButton(false)
            setShowSubtitleButton(false)
            setShowShuffleButton(false)
            findViewById<View>(com.google.android.exoplayer2.ui.R.id.exo_settings).visibility = View.GONE
        }
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                playerView.onResume()
                player.playWhenReady = autoPlay.value
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop() {
                updateState()
                playerView.onPause()
                player.playWhenReady = false
            }
        })
        playerView
    }

    DisposableEffect(Unit) {
        onDispose {
            updateState()
            player.release()
        }
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxWidth()
    ) {
        playerView.player = player
    }
}