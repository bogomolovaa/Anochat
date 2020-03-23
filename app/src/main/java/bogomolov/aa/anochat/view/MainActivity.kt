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
import bogomolov.aa.anochat.android.DiscoveryTest2
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.*
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

            if (destination.id != R.id.signInFragment) {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val signedIn = viewModel.isSignedIn()
                    Log.i("test", "signedIn $signedIn")
                    if (!signedIn) {
                        //withContext(Dispatchers.Main) {
                        controller.navigate(R.id.signInFragment)
                        //}
                    }
                }
            }

        }

        //setSetting(this, UID,"LX4U2yR5ZJUsN5hivvDvF9NUHXJ3")
        viewModel.startWorkManager()

        GlobalScope.launch(Dispatchers.IO) {
            doDiscovery()
        }
        //val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        //val editor: Editor = sharedPreferences.edit()
        //editor.clear()
        //editor.commit()
    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(applicationContext)
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }

    private fun doDiscovery() {
        try {
            val ip = getLocalIpAddress()
            Log.i("test", "ip $ip")
            val dt = DiscoveryTest2(
                InetAddress.getByName(ip), 1549,
                //"stun.sipnet.ru", 3478  //
            //"stunserver.org",  3478
                "stun.voippro.com", 3478
            )
            val di = dt.test()
            Log.i("test", "$di")
            Log.i("test", "public port ${di?.publicPort}")
        } catch (ex: UnknownHostException) {
            Log.i(
                "test",
                "Could not resolve STUN server. Make sure that a network connection is available"
            )
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.i("test", ex.message ?: "null")
        }

    }

    fun getLocalIpAddress(): String? {
        try {
            val en =
                NetworkInterface.getNetworkInterfaces()
            var lastAddress: String? = null
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        //return inetAddress.hostAddress
                        lastAddress = inetAddress.hostAddress
                        Log.i("test","inetAddress.hostAddress ${inetAddress.hostAddress}")
                    }
                }
            }
            return lastAddress
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }


}






