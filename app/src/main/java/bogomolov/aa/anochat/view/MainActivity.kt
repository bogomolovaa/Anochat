package bogomolov.aa.anochat.view

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.ActivityMainBinding
import bogomolov.aa.anochat.viewmodel.MainActivityViewModel
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

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: MainActivityViewModel by viewModels { viewModelFactory }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        emojiSupport()

        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val currentDestination = controller.currentDestination
            //Log.i("test", "currentDestination $currentDestination")
            if (currentDestination != null) {
                //Log.i("test", "destination $destination")
                if (currentDestination.id != R.id.signInFragment) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
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






