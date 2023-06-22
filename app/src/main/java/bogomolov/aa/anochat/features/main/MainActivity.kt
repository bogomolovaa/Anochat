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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.work.*
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.features.contacts.UpdateWorker
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.repository.Firebase
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
        startWorkManager()
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
                navController.navigate(Route.Users.route(uri = URLEncoder.encode(uri.toString(), "utf-8")))
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
                "image?name={name}", "video?uri={uri}" -> setFullScreen(true)
                else -> setFullScreen(false)
            }
            if (destination.route != "login" && !authRepository.isSignedIn())
                navigateToSignIn(controller)
        }
    }

    private fun navigateToSignIn(navController: NavController) {
        onPostResume()
        navController.navigate(Route.Login.route) { popUpTo(Route.Login.route) }
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

    private fun setFullScreen(enabled: Boolean) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }.apply {
            WindowInsetsCompat.Type.systemBars().let { if (enabled) hide(it) else show(it) }
        }
    }
}