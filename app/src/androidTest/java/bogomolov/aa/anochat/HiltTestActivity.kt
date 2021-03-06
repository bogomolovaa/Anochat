package bogomolov.aa.anochat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController


        emojiSupport()
    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(applicationContext)
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }
}