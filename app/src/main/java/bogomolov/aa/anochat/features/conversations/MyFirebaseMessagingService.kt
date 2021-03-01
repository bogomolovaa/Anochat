package bogomolov.aa.anochat.features.conversations

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.getBitmap
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
            serviceScope.launch(Dispatchers.IO) {
                when (type) {
                    TYPE_KEY -> messageUseCases.finallyReceivedPublicKey(publicKey!!, uid!!)
                    TYPE_INIT_KEY -> messageUseCases.receivedPublicKey(publicKey!!, uid!!)
                    TYPE_MESSAGE -> receiveMessage(data)
                    TYPE_REPORT -> messageUseCases.receiveReport(messageId!!, received, viewed)
                }
            }
        }
    }

    private suspend fun receiveMessage(data: Map<String, String>) {
        val text = data["body"] ?: ""
        val replyId = data["reply"]
        val messageId = data["messageId"]
        val uid = data["source"]
        var image = data["image"]
        var audio = data["audio"]
        if (image.isNullOrEmpty()) image = null
        if (audio.isNullOrEmpty()) audio = null
        if (uid != null && messageId != null) {
            val message = Message(
                text = text,
                time = System.currentTimeMillis(),
                messageId = messageId,
                replyMessageId = replyId,
                image = image,
                audio = audio
            )
            messageUseCases.receiveMessage(message, uid) {
                showNotification(it)
            }
        }
    }

    private fun showNotification(message: Message) {
        val inBackground = (application as AnochatAplication).inBackground
        val settings = authRepository.getSettings()
        if (inBackground && settings.notifications) {
            val conversation =
                conversationUseCases.getConversation(message.conversationId)
            sendNotification(message, conversation.user, settings)
        }
    }

    private fun sendNotification(message: Message, user: User, settings: Settings) {
        val context = applicationContext
        val bitmap = if (user.photo != null)
            getBitmap(getMiniPhotoFileName(user.photo), context)
        else
            BitmapFactory.decodeResource(context.resources, R.drawable.user_icon)
        val pendingIntent = NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.conversationFragment)
            .setArguments(Bundle().apply { putLong("id", message.conversationId) })
            .createPendingIntent()
        val channelId = "anochat channel"
        val title = user.name
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setLargeIcon(bitmap)
                .setLights(Color.RED, 3000, 3000)
                .setContentText(message.shortText())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        if (settings.vibration) notificationBuilder.setVibrate(longArrayOf(500, 500))
        if (settings.sound) notificationBuilder.setSound(
            android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
            AudioManager.STREAM_NOTIFICATION
        )
        val image = message.image
        if (image != null) {
            val imageBitmap = getBitmap(image, context, 4)
            if (imageBitmap != null) notificationBuilder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
            )
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}