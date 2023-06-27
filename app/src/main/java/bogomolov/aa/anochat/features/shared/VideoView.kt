package bogomolov.aa.anochat.features.shared

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import bogomolov.aa.anochat.features.main.LocalNavController
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView

@Composable
fun VideoView(uri: String) {
    val context = LocalContext.current
    var window by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0L) }
    var autoPlay by remember { mutableStateOf(true) }
    var player by remember { mutableStateOf<SimpleExoPlayer?>(null) }
    val navController = LocalNavController.current
    LaunchedEffect(Unit) {
        val mediaItem = MediaItem.fromUri(uri)
        player = SimpleExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            playWhenReady = true
            seekTo(window, position)
            prepare()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun updateState() {
        autoPlay = player?.playWhenReady ?: false
        window = player?.currentWindowIndex ?: 0
        position = 0L.coerceAtLeast(player?.contentPosition ?: 0L)
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
        playerView
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {

            override fun onStart(owner: LifecycleOwner) {
                playerView.onResume()
                player?.playWhenReady = autoPlay
            }

            override fun onStop(owner: LifecycleOwner) {
                updateState()
                playerView.onPause()
                player?.playWhenReady = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BackButton(onClick = remember { { navController?.popBackStack() } })
        }
    }
}