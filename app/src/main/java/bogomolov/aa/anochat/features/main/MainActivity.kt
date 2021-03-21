package bogomolov.aa.anochat.features.main

import android.app.NotificationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.work.*
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.ActivityMainBinding
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.features.contacts.UpdateWorker
import bogomolov.aa.anochat.features.shared.AuthRepository
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var authRepository: AuthRepository

    @Inject
    internal lateinit var userUseCases: UserUseCases

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityMainBinding.inflate(layoutInflater)
        emojiSupport()
        addSignInListener()
        startWorkManager()
    }

    override fun onStart() {
        super.onStart()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    private fun addSignInListener() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            if (destination.id != R.id.signInFragment && !authRepository.isSignedIn())
                navigateToSignIn(controller, destination)
        }
    }

    private fun navigateToSignIn(navController: NavController, destination: NavDestination) {
        val navOptions =
            NavOptions.Builder().setPopUpTo(destination.id, true).build()
        onPostResume()
        navController.navigate(R.id.signInFragment, null, navOptions)
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
            "updateUsers",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWorkRequest
        )
    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(applicationContext)
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }
}