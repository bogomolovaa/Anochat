package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.shared.mvi.ActionExecutor
import java.text.SimpleDateFormat
import java.util.*

class PlayAudioView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var audioFile: String? = null
    private var messageId: String? = null
    lateinit var actionExecutor: ActionExecutor

    init {
        inflate(context, R.layout.play_audio_layout, this)
    }

    fun set(audioFile: String?, messageId: String? = null) {
        this.audioFile = audioFile
        this.messageId = messageId
        setInitialState()
    }

    fun setOnCloseListener(onClose: () -> Unit) {
        val closeIcon: ImageView = findViewById(R.id.closePlayImage)
        closeIcon.visibility = View.VISIBLE
        closeIcon.setOnClickListener { onClose() }
    }

    fun setInitialState() {
        val playPauseButton: ImageView = findViewById(R.id.playPause)
        if (audioFile != null) {
            playPauseButton.setImageResource(R.drawable.send_icon)
            playPauseButton.setOnClickListener {
                playPauseButton.setImageResource(R.drawable.pause_icon)
                actionExecutor.addAction(StartPlayingAction(audioFile, messageId))
            }
        }else{
            playPauseButton.setImageResource(R.drawable.ic_error_outline)
        }
    }

    fun setPlayingState(state: PlayingState) {
        val lengthText: TextView = findViewById(R.id.length_text)
        val progressBar: ProgressBar = findViewById(R.id.playProgressBar)
        val timerText: TextView = findViewById(R.id.timer_text)
        progressBar.max = (state.duration / 1000).toInt()
        progressBar.progress = (state.elapsed / 1000).toInt()
        lengthText.text = timeToString(state.duration)
        timerText.text = timeToString(state.elapsed)
        val playPauseButton: ImageView = findViewById(R.id.playPause)
        if (state.paused) {
            playPauseButton.setImageResource(R.drawable.send_icon)
            playPauseButton.setOnClickListener {
                actionExecutor.addAction(StartPlayingAction(audioFile, messageId))
            }
        } else {
            playPauseButton.setImageResource(R.drawable.pause_icon)
            playPauseButton.setOnClickListener {
                actionExecutor.addAction(PausePlayingAction())
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeToString(time: Long) = SimpleDateFormat("mm:ss").format(Date(time))
}