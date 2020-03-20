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
@Inject constructor(private val repository: Repository) : ViewModel() {

    fun sendMessage(imageFileName: String, messageText: String, conversationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val message = Message(
                text = messageText,
                time = System.currentTimeMillis(),
                conversationId = conversationId,
                image = imageFileName
            )
            repository.saveMessage(message, conversationId)
            val conversation = repository.getConversation(conversationId)
            if (repository.uploadFile(imageFileName,conversation.user.uid, true)) {
                repository.sendMessage(message)
            } else {
                Log.i("test", "Not uploaded")
            }
        }
    }

}