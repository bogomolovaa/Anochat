package bogomolov.aa.anochat.features.conversations

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkBuilder
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.repository.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Long
import java.util.*
import javax.inject.Inject

private const val TYPE_MESSAGE = "message"
private const val TYPE_REPORT = "report"
private const val TYPE_KEY = "key"
private const val TYPE_INIT_KEY = "init_key"
private const val TAG = "FirebaseService"

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var messageUseCases: MessageUseCases

    @Inject
    lateinit var conversationUseCases: ConversationUseCases

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var firebase: Firebase


    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)


    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val data = remoteMessage.data
            val type = data["type"] ?: TYPE_MESSAGE
            val uid = data["source"]
            val publicKey = data["key"]
            val received = data["received"]?.toInt() ?: 0
            val viewed = data["viewed"]?.toInt() ?: 0
            val messageId = data["messageId"]
            if (messageId != null)
                serviceScope.launch(Dispatchers.IO) {
                    when (type) {
                        TYPE_KEY -> {
                            firebase.deleteRemoteMessage(messageId)
                            Log.d(TAG, "finallyReceivedPublicKey from $uid")
                            messageUseCases.finallyReceivedPublicKey(publicKey!!, uid!!)
                        }
                        TYPE_INIT_KEY -> {
                            firebase.deleteRemoteMessage(messageId)
                            Log.d(TAG, "receivedPublicKey from $uid")
                            messageUseCases.receivedPublicKey(publicKey!!, uid!!)
                        }
                        TYPE_MESSAGE -> receiveMessage(data)
                        TYPE_REPORT -> {
                            if (viewed == 1 || received == -1)
                                firebase.deleteRemoteMessage(messageId)
                            Log.d(TAG, "receivedReport received $received viewed $viewed")
                            messageUseCases.receiveReport(messageId, received, viewed)
                        }
                    }
                }
        }
    }

    private suspend fun receiveMessage(data: Map<String, String>) {
        val text = data["body"] ?: ""
        val replyId = data["reply"]
        val messageId = data["messageId"]
        val uid = data["source"]
        var file = data["image"]
        var audio = data["audio"]
        if (file.isNullOrEmpty()) file = null
        if (audio.isNullOrEmpty()) audio = null
        if (uid != null && messageId != null) {
            var image: String? = null
            var video: String? = null
            if (file != null)
                if (file.endsWith(".jpg")) {
                    image = file
                } else {
                    video = file
                }
            val message = Message(
                text = text,
                time = System.currentTimeMillis(),
                messageId = messageId,
                replyMessageId = replyId,
                image = image,
                audio = audio,
                video = video
            )
            tempExtractTime(message)
            messageUseCases.receiveMessage(message, uid) {
                showNotification(it)
                playSound()
            }
        }
    }

    private fun tempExtractTime(message: Message) {
        val timePart = message.text.substring(message.text.length - 13)
        message.time = Long.parseLong(timePart)
        message.text = message.text.substring(0, message.text.length - 13)
        Log.i("test", "tempExtractTime ${Date(message.time)}")
    }

    private fun showNotification(message: Message) {
        val inBackground = (application as AnochatAplication).inBackground
        val settings = authRepository.getSettings()
        if (inBackground && settings.notifications) {
            val conversation =
                conversationUseCases.getConversation(message.conversationId)
            if (conversation != null) sendNotification(message, conversation.user, settings)
        }
    }

    private fun playSound() {
        val inForeground = !(application as AnochatAplication).inBackground
        if (inForeground) playMessageSound(this)
    }

    private fun sendNotification(message: Message, user: User, settings: Settings) {
        val context = applicationContext
        val bitmap = if (user.photo != null)
            getBitmap(getMiniPhotoFileName(user.photo), context)
        else
            BitmapFactory.decodeResource(context.resources, R.drawable.user_icon)
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "anochat://anochat/conversation/${message.conversationId}".toUri(),
            context,
            MainActivity::class.java
        )
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val channelId = "anochat channel"
        val title = user.name
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(bitmap)
                .setLights(Color.RED, 3000, 3000)
                .setContentText(message.shortText())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        if (settings.vibration) notificationBuilder.setVibrate(longArrayOf(500, 500))
        if (settings.sound)
            notificationBuilder.setSound(DEFAULT_NOTIFICATION_URI, AudioManager.STREAM_NOTIFICATION)
        val image = message.image
        if (image != null) {
            val imageBitmap = getBitmapFromGallery(image, context, 4)
            if (imageBitmap != null)
                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle().bigPicture(imageBitmap)
                )
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            val soundAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.setSound(DEFAULT_NOTIFICATION_URI, soundAttributes)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}