package bogomolov.aa.anochat.android

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.repository.Repository
import bogomolov.aa.anochat.view.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TYPE_MESSAGE = "message"
private const val TYPE_READ_REPORT = "report"

class MyFirebaseMessagingService : FirebaseMessagingService(), HasAndroidInjector {

    @Inject
    lateinit var repository: Repository
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("test", "From: " + remoteMessage.getFrom())

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("test", "Message data payload: " + remoteMessage.data);
            val data = remoteMessage.data
            val type = data["type"] ?: TYPE_MESSAGE
            val messageId = data["messageId"]
            when (type) {
                TYPE_MESSAGE -> {
                    val text = data["body"] ?: ""
                    val uid = data["source"]
                    var image = data["image"]
                    val replyId = data["reply"]
                    if (image.isNullOrEmpty()) image = null
                    if (uid != null && messageId != null)
                        GlobalScope.launch(Dispatchers.IO) {
                            Log.i("test", "receiveMessage");
                            val message = repository.receiveMessage(text, uid, messageId, replyId, image)
                            if (message != null) {
                                val inBackground = (application as AnochatAplication).inBackground
                                if (message.image != null) repository.downloadFile(message.image)
                                if (inBackground) sendNotification(message)
                            }
                        }
                }
                TYPE_READ_REPORT -> {
                    val received = data["received"]?.toInt() ?: 0
                    val viewed = data["viewed"]?.toInt() ?: 0
                    Log.i("test", "receiveReport received $received");
                    if (messageId != null) {
                        repository.receiveReport(messageId, received, viewed)
                        if (viewed == 1)
                            GlobalScope.launch(Dispatchers.IO) {
                                repository.deleteRemoteMessage(messageId)
                            }
                    }
                }
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val body = remoteMessage.notification?.body;
            Log.d("test", "Message Notification Body: $body")
        }

    }

    override fun onNewToken(token: String) {
    }

    private fun sendNotification(message: Message) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = "annochat channel"
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("You have a new message")
                .setSmallIcon(R.mipmap.sym_def_app_icon)
                .setContentText(message.text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}