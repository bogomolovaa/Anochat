package bogomolov.aa.anochat.features.main

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.navigation.NavController
import androidx.work.*
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.features.contacts.UpdateWorker
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.repository.Firebase
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var authRepository: AuthRepository

    @Inject
    internal lateinit var userUseCases: UserUseCases

    @Inject
    lateinit var firebase: Firebase

    private var navController: NavController? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navController?.let { onSendAction(it, intent) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //emojiSupport()
        startWorkManager()

        setContent {
            NavGraph({ this@MainActivity }) {
                addSignInListener(it)
                onSendAction(it, intent)
                this@MainActivity.navController = it
            }
        }
    }

    private fun onSendAction(navController: NavController, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                navController.navigate("users?uri=${URLEncoder.encode(uri.toString(), "utf-8")}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        (application as AnochatAplication).inBackground = false
        firebase.setOnline()
    }

    override fun onPause() {
        super.onPause()
        (application as AnochatAplication).inBackground = true
        firebase.setOffline()
    }

    private fun addSignInListener(navController: NavController) {
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            when (destination.route) {
                "image?name={name}", "video?uri={uri}" -> setFullScreen()
                else -> removeFullScreen()
            }
            if (destination.route != "login" && !authRepository.isSignedIn())
                navigateToSignIn(controller)
        }
    }

    private fun navigateToSignIn(navController: NavController) {
        onPostResume()
        navController.navigate("login") { popUpTo("login") { inclusive = false } }
    }

    private fun startWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(application).enqueueUniquePeriodicWork(
            "update",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            uploadWorkRequest
        )
    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(applicationContext)
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }

    private fun setFullScreen() {
        window.apply {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setDecorFitsSystemWindows(true)
        }
    }

    private fun removeFullScreen() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
}