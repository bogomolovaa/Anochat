package bogomolov.aa.anochat.features.shared

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val TAG = "AudioPlayer"

interface AudioPlayer {
    fun initPlayer(audioFile: String, onComplete: () -> Unit): Long
    fun startPlay(): Boolean
    fun pausePlay(): Boolean
    fun startRecording(): String
    fun stopRecording()
}

class AudioPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayer {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun initPlayer(audioFile: String, onComplete: () -> Unit): Long {
        val player = MediaPlayer()
        val filePath = getFilePath(context, audioFile)
        if (File(filePath).exists()) {
            player.setDataSource(filePath)
            player.prepare()
            val duration = player.duration.toLong()
            player.setOnCompletionListener {
                onComplete()
                mediaPlayer = null
            }
            mediaPlayer = player
            return duration
        } else {
            Log.w(TAG, "error file: $filePath not exist")
        }
        return 0
    }

    override fun startPlay(): Boolean {
        mediaPlayer?.start() ?: return false
        return true
    }

    override fun pausePlay(): Boolean {
        mediaPlayer?.pause() ?: return false
        return true
    }

    override fun startRecording(): String {
        val audioFile = "${getRandomFileName()}.3gp"
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setOutputFile(getFilePath(context, audioFile))
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.prepare()
        recorder.start()
        mediaRecorder = recorder
        return audioFile
    }

    override fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
    }
}