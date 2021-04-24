package bogomolov.aa.anochat.features.shared

import android.content.Context
import android.media.MediaPlayer
import bogomolov.aa.anochat.R

fun playMessageSound(context: Context) {
    val player = MediaPlayer.create(context, R.raw.message2)
    player.setVolume(0.05f, 0.05f)
    player.start()
}