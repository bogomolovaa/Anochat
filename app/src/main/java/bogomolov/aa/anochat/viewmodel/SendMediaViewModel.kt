package bogomolov.aa.anochat.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.android.MAX_IMAGE_DIM
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.android.getRandomString
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max

class SendMediaViewModel
@Inject constructor(val repository: Repository) : ViewModel() {

    fun resizeImage(path: String): File {
        val newFileName = getRandomString(20) + ".jpg"
        val bitmap = BitmapFactory.decodeFile(path)
        var ratio = MAX_IMAGE_DIM / max(bitmap.width, bitmap.height).toFloat()
        if (ratio > 1) ratio = 1.0f
        val resized = Bitmap.createScaledBitmap(
            bitmap,
            (ratio * bitmap.width).toInt(),
            (ratio * bitmap.height).toInt(),
            true
        )
        val file = File(getFilesDir(repository.getContext()), "$newFileName.jpg")
        try {
            val stream = FileOutputStream(file)
            resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    fun sendMessage(imageFileName: String, messageText: String, conversationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val message = Message(
                text = messageText,
                time = System.currentTimeMillis(),
                conversationId = conversationId,
                image = imageFileName
            )
            repository.saveMessage(message, conversationId)
            if (repository.uploadFile(imageFileName)) {
                repository.sendMessage(message, repository.getConversation(conversationId).user.uid)
            } else {
                Log.i("test", "Not uploaded")
            }
        }
    }

}