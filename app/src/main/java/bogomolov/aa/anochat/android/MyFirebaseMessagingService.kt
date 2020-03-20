package bogomolov.aa.anochat.android

import android.app.NotificationChannel
import android.media.AudioManager
import bogomolov.aa.anochat.R

import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
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
private const val TYPE_KEY = "key"
private const val TYPE_INIT_KEY = "init_key"
private const val TAG = "FirebaseService"

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
        val context = repository.getContext()
        val myUid = getSetting<String>(context, UID)!!

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("test", "Message data payload: " + remoteMessage.data);
            val data = remoteMessage.data
            val type = data["type"] ?: TYPE_MESSAGE
            val messageId = data["messageId"]
            val uid = data["source"]
            val key = data["key"]
            when (type) {
                TYPE_KEY -> {
                    if (uid != null && key != null) {
                        GlobalScope.launch(Dispatchers.IO) {
                            val privateKey = getPrivateKey(myUid, uid, context)
                            if (privateKey != null) {
                                Log.i("test", "privateKey NOT null, send messages")
                                generateAndSaveSecretKey(privateKey, key, myUid, uid, context)
                                for (message in repository.getPendingMessages(uid))
                                    repository.sendMessage(message)
                            } else {
                                Log.i(TAG, "secret key not generated: privateKey null")
                            }
                        }
                    }
                }
                TYPE_INIT_KEY -> {
                    if (uid != null && key != null) {
                        GlobalScope.launch(Dispatchers.IO) {
                            Log.i("test", "privateKey null")
                            repository.sendPublicKey(uid,false)
                            val privateKey = getPrivateKey(myUid, uid, context)
                            generateAndSaveSecretKey(privateKey!!, key, myUid, uid, context)
                        }
                    }
                }
                //https://stackoverflow.com/questions/13261252/javax-crypto-illegalblocksizeexception-last-block-incomplete-in-decryption-de
                //https://stackoverflow.com/questions/18350459/javax-crypto-illegalblocksizeexception-last-block-incomplete-in-decryption-exce/20417874#20417874
                TYPE_MESSAGE -> {
                    var text = data["body"] ?: ""
                    val uid = data["source"]
                    var image = data["image"]
                    var audio = data["audio"]
                    val replyId = data["reply"]
                    if (image.isNullOrEmpty()) image = null
                    if (audio.isNullOrEmpty()) audio = null
                    if (uid != null && messageId != null)
                        GlobalScope.launch(Dispatchers.IO) {
                            Log.i("test", "receiveMessage")
                            val secretKey = getSecretKey(myUid, uid, context)
                            if (secretKey == null) {
                                Log.i("test", "not received message: null secretKey")
                                repository.sendPublicKey(uid, true)
                            } else {
                                val message = repository.receiveMessage(
                                    text,
                                    uid,
                                    messageId,
                                    replyId,
                                    image,
                                    audio
                                )
                                if (message != null) {
                                    val inBackground =
                                        (application as AnochatAplication).inBackground
                                    val showNotification =
                                        getSetting<Boolean>(
                                            applicationContext,
                                            NOTIFICATIONS
                                        ) != null
                                    if (inBackground && showNotification) sendNotification(message)
                                }
                            }
                        }
                }
                TYPE_READ_REPORT -> {
                    val received = data["received"]?.toInt() ?: 0
                    val viewed = data["viewed"]?.toInt() ?: 0
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

    private suspend fun sendNotification(message: Message) {
        val conversation = repository.getConversation(message.conversationId)
        val bitmap = if (conversation.user.photo != null)
            BitmapFactory.decodeFile(getFilePath(applicationContext, conversation.user.photo!!))
        else
            BitmapFactory.decodeResource(applicationContext.resources, R.drawable.user_icon)
        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.conversationFragment)
            .setArguments(Bundle().apply { putLong("id", message.conversationId) })
            .createPendingIntent()
        val channelId = "anochat channel"
        val title = conversation.user.name
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setLargeIcon(bitmap)
                .setLights(Color.RED, 3000, 3000)
                .setContentText(message.shortText())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        val canVibrate = getSetting<Boolean>(applicationContext, VIBRATION) != null
        if (canVibrate) notificationBuilder.setVibrate(longArrayOf(1000, 1000))
        val canSound = getSetting<Boolean>(applicationContext, SOUND) != null
        if (canSound) notificationBuilder.setSound(
            android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
            AudioManager.STREAM_NOTIFICATION
        )

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