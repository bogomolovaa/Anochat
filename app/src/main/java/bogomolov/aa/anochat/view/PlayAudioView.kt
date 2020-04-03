package bogomolov.aa.anochat.view

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PlayAudioView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var player: MediaPlayer? = null
    private var playJob: Job? = null
    private var startTime: Long? = null
    private var pastDuration = 0L


    init {
        inflate(context, R.layout.play_audio_layout, this)
    }

    fun setFile(fileName: String) {
        val timerText: TextView = findViewById(R.id.timer_text)
        val lengthText: TextView = findViewById(R.id.length_text)
        val progressBar: ProgressBar = findViewById(R.id.playProgressBar)
        val player = MediaPlayer()
        Log.i("test","audio file: "+File(getFilesDir(context), fileName).path)
        if(File(getFilesDir(context), fileName).exists()) {
            player.setDataSource(File(getFilesDir(context), fileName).path)
            player.prepare()
            val duration = player.duration.toLong()
            progressBar.max = (duration / 1000).toInt()
            lengthText.text = timeToString(duration)
            this.player = player
            val playPauseButton: ImageView = findViewById(R.id.playPause)
            playPauseButton.setOnClickListener {
                start()
            }
            player.setOnCompletionListener {
                startTime = null
                pastDuration = 0
                progressBar.progress = 0
                timerText.text = "0:00"
                playPauseButton.setImageResource(R.drawable.send_icon)
                playPauseButton.setOnClickListener {
                    start()
                }
                if (playJob != null) playJob!!.cancel()
            }
        }else{
            Log.i("test","error file: "+File(getFilesDir(context), fileName).path+" not exist")
        }
        timerText.text = "0:00"
    }


    @SuppressLint("SimpleDateFormat")
    private fun timeToString(time: Long) = SimpleDateFormat("mm:ss").format(Date(time))

    private fun start() {
        val progressBar: ProgressBar = findViewById(R.id.playProgressBar)
        val timerText: TextView = findViewById(R.id.timer_text)
        startTime = System.currentTimeMillis()
        playJob = GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                val time = System.currentTimeMillis() - startTime!! + pastDuration
                progressBar.progress = (time / 1000).toInt()
                timerText.text = timeToString(time)
                delay(1000)
            }
        }
        player!!.start()
        val playPauseButton: ImageView = findViewById(R.id.playPause)
        playPauseButton.setImageResource(R.drawable.pause_icon)
        playPauseButton.setOnClickListener {
            pause()
        }
    }

    private fun pause() {
        pastDuration += System.currentTimeMillis() - startTime!!
        if (playJob != null) playJob!!.cancel()
        player!!.pause()
        val playPauseButton: ImageView = findViewById(R.id.playPause)
        playPauseButton.setImageResource(R.drawable.send_icon)
        playPauseButton.setOnClickListener {
            start()
        }
    }

    fun setOnClose(onClose: () -> Unit) {
        val closeIcon: ImageView = findViewById(R.id.closePlayImage)
        closeIcon.visibility = View.VISIBLE
        closeIcon.setOnClickListener { onClose() }
    }
}

@BindingAdapter("app:fileName")
fun setFileName(view: PlayAudioView, fileName: String?) {
    if (!fileName.isNullOrEmpty()) {
        view.setFile(fileName)
        view.requestLayout()
    }
}