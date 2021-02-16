package bogomolov.aa.anochat.features.main

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.work.*
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.MyWorkerFactory
import bogomolov.aa.anochat.databinding.ActivityMainBinding
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.features.contacts.UpdateWorker
import bogomolov.aa.anochat.features.shared.AuthRepository
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    internal lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject
    internal lateinit var authRepository: AuthRepository
    @Inject
    internal lateinit var userUseCases: UserUseCases

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityMainBinding.inflate(layoutInflater)
        emojiSupport()
        addSignInListener()
        startWorkManager()
    }

    private fun addSignInListener(){
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            val currentDestination = controller.currentDestination
            Log.i("MainActivity", "currentDestination $currentDestination")
            if (currentDestination != null) {
                Log.i("MainActivity", "destination $destination")
                if (currentDestination.id != R.id.signInFragment) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val signedIn = authRepository.isSignedIn()
                        //Log.i("test", "signedIn $signedIn")
                        if (!signedIn) {
                            withContext(Dispatchers.Main) {
                                val navOptions =
                                    NavOptions.Builder().setPopUpTo(destination.id, true).build()
                                //Log.i("test", "controller.navigate R.id.signInFragment")
                                super.onPostResume()
                                controller.navigate(R.id.signInFragment, null, navOptions)
                            }
                        }
                    }
                }
            }

        }
    }

    private fun clearPreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun startWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        initializeWorkManager().enqueueUniquePeriodicWork(
            "updateUsers",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWorkRequest
        )
    }

    private fun initializeWorkManager(): WorkManager {
        val appContext = application as AnochatAplication
        val factory = appContext.workManagerConfiguration.workerFactory
                as DelegatingWorkerFactory
        factory.addFactory(MyWorkerFactory(userUseCases))

        return WorkManager.getInstance(appContext)
    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(applicationContext)
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }

}






