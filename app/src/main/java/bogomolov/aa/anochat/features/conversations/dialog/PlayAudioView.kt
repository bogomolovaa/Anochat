package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.shared.getFilePath
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val START_TIMER_TEXT = "0:00"
private const val TAG = "PlayAudioView"

class PlayAudioView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var player: MediaPlayer? = null
    private var playJob: Job? = null
    private var startTime: Long? = null
    private var pastDuration = 0L

    init {
        inflate(context, R.layout.play_audio_layout, this)
    }

    fun setOnCloseListener(onClose: () -> Unit) {
        val closeIcon: ImageView = findViewById(R.id.closePlayImage)
        closeIcon.visibility = View.VISIBLE
        closeIcon.setOnClickListener { onClose() }
    }

    fun setFile(fileName: String) {
        val timerText: TextView = findViewById(R.id.timer_text)
        val lengthText: TextView = findViewById(R.id.length_text)
        val progressBar: ProgressBar = findViewById(R.id.playProgressBar)
        val player = MediaPlayer()
        val filePath = getFilePath(context, fileName)
        if (File(filePath).exists()) {
            player.setDataSource(filePath)
            player.prepare()
            val duration = player.duration.toLong()
            progressBar.max = (duration / 1000).toInt()
            lengthText.text = timeToString(duration)
            this.player = player
            val playPauseButton: ImageView = findViewById(R.id.playPause)
            playPauseButton.setOnClickListener { start() }
            player.setOnCompletionListener {
                startTime = null
                pastDuration = 0
                progressBar.progress = 0
                timerText.text = START_TIMER_TEXT
                playPauseButton.setImageResource(R.drawable.send_icon)
                playPauseButton.setOnClickListener { start() }
                if (playJob != null) playJob!!.cancel()
            }
        } else {
            Log.w(TAG, "error file: $filePath not exist")
        }
        timerText.text = START_TIMER_TEXT
    }

    private fun start() {
        val player = this.player
        if (player != null) {
            val progressBar: ProgressBar = findViewById(R.id.playProgressBar)
            val timerText: TextView = findViewById(R.id.timer_text)
            startTime = System.currentTimeMillis()
            //todo: start in viewModelScope
            playJob = GlobalScope.launch(Dispatchers.Main) {
                while (true) {
                    val time = System.currentTimeMillis() - startTime!! + pastDuration
                    progressBar.progress = (time / 1000).toInt()
                    timerText.text = timeToString(time)
                    delay(1000)
                }
            }
            player.start()
            val playPauseButton: ImageView = findViewById(R.id.playPause)
            playPauseButton.setImageResource(R.drawable.pause_icon)
            playPauseButton.setOnClickListener { pause() }
        }else{
            Log.w(TAG, "Can't start, null player")
        }
    }

    private fun pause() {
        val player = this.player
        if (player != null) {
            pastDuration += System.currentTimeMillis() - startTime!!
            playJob?.cancel()
            player.pause()
            val playPauseButton: ImageView = findViewById(R.id.playPause)
            playPauseButton.setImageResource(R.drawable.send_icon)
            playPauseButton.setOnClickListener { start() }
        }else{
            Log.w(TAG, "Can't pause, null player")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeToString(time: Long) = SimpleDateFormat("mm:ss").format(Date(time))
}

@BindingAdapter("app:fileName")
fun setFileName(view: PlayAudioView, fileName: String?) {
    if (!fileName.isNullOrEmpty()) {
        view.setFile(fileName)
        view.requestLayout()
    }
}