package bogomolov.aa.anochat.view

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.UID
import bogomolov.aa.anochat.android.setSetting
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
import javax.inject.Inject


class MainActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    internal lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: MainActivityViewModel by viewModels { viewModelFactory }
    lateinit var navController: NavController
    var conversationId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        emojiSupport()

        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->

            if (destination.id != R.id.signInFragment && destination.id != R.id.signUpFragment) {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val signedIn = viewModel.isSignedIn()
                    Log.i("test", "signedIn $signedIn")
                    if (!signedIn) controller.navigate(R.id.signInFragment)
                }
            }

        }

        //setSetting(this, UID,"LX4U2yR5ZJUsN5hivvDvF9NUHXJ3")
        viewModel.startWorkManager()
    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }

}






