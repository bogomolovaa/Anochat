package bogomolov.aa.anochat.repository

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.features.shared.*
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

open class NotificationsService @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val conversationUseCases: ConversationUseCases
) {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    fun showNotification(message: Message) {
        serviceScope.launch {
            val inBackground = (application as AnochatAplication).inBackground
            val settings = authRepository.getSettings()
            if (inBackground) {
                if (settings.notifications)
                    conversationUseCases.getConversation(message.conversationId)?.let {
                        sendNotification(message, it.user, settings)
                    }
            } else {
                playMessageSound(application)
            }
        }
    }

    private fun sendNotification(message: Message, user: User, settings: Settings) {
        val context = application
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
            NotificationCompat.Builder(application, channelId)
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
            notificationBuilder.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioManager.STREAM_NOTIFICATION
            )
        val image = message.image
        if (image != null) {
            val imageBitmap = getBitmapFromGallery(image, context, 4)
            if (imageBitmap != null)
                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle().bigPicture(imageBitmap)
                )
        }

        val notificationManager =
            application.getSystemService(FirebaseMessagingService.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            val soundAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                soundAttributes
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}