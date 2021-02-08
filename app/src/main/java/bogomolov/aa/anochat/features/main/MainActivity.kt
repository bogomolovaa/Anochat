package bogomolov.aa.anochat.features.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.ActivityMainBinding
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    internal lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: MainActivityViewModel by viewModels { viewModelFactory }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        emojiSupport()

        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            val currentDestination = controller.currentDestination
            //Log.i("test", "currentDestination $currentDestination")
            if (currentDestination != null) {
                //Log.i("test", "destination $destination")
                if (currentDestination.id != R.id.signInFragment) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val signedIn = viewModel.isSignedIn()
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


        //setSetting(this, UID,"LX4U2yR5ZJUsN5hivvDvF9NUHXJ3")
        viewModel.startWorkManager()


        /*
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.clear()
        editor.commit()
        */

    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(applicationContext)
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }


    override fun onStart() {
        super.onStart()
        viewModel.setOnline()
    }

    override fun onStop() {
        super.onStop()
        viewModel.setOffline()
    }
}






