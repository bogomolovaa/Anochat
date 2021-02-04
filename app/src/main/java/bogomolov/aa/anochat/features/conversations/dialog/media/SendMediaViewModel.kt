package bogomolov.aa.anochat.features.conversations.dialog.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            }
        }
    }

}