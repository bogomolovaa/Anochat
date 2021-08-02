package bogomolov.aa.anochat.features.shared

import android.net.Uri
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView

@Composable
fun VideoView(uri: Uri) {
    val context = LocalContext.current
    val window = remember { mutableStateOf(0) }
    val position = remember { mutableStateOf(0L) }
    val autoPlay = remember { mutableStateOf(true) }
    var player by remember { mutableStateOf<SimpleExoPlayer?>(null) }
    LaunchedEffect(0) {
        val mediaItem = MediaItem.fromUri(uri)

        player = SimpleExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            playWhenReady = true
            seekTo(window.value, position.value)
            prepare()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun updateState() {
        autoPlay.value = player?.playWhenReady ?: false
        window.value = player?.currentWindowIndex ?: 0
        position.value = 0L.coerceAtLeast(player?.contentPosition ?: 0L)
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
                player?.playWhenReady = autoPlay.value
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop() {
                updateState()
                playerView.onPause()
                player?.playWhenReady = false
            }
        })
        playerView
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            playerView.player = player
        }
    }
}