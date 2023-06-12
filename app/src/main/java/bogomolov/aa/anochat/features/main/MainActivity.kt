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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.net.toUri
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.features.contacts.UpdateWorker
import bogomolov.aa.anochat.features.contacts.list.UsersView
import bogomolov.aa.anochat.features.contacts.user.UserView
import bogomolov.aa.anochat.features.conversations.dialog.ConversationView
import bogomolov.aa.anochat.features.conversations.dialog.SendMediaView
import bogomolov.aa.anochat.features.conversations.list.ConversationsView
import bogomolov.aa.anochat.features.login.SignInView
import bogomolov.aa.anochat.features.settings.MiniatureView
import bogomolov.aa.anochat.features.settings.SettingsView
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.ImageView
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.VideoView
import bogomolov.aa.anochat.repository.Firebase
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

val LocalNavController = compositionLocalOf<NavHostController?> { null }

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var authRepository: AuthRepository

    @Inject
    internal lateinit var userUseCases: UserUseCases

    @Inject
    lateinit var firebase: Firebase

    @ExperimentalMaterialApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //emojiSupport()
        startWorkManager()
        //setFullScreen()

        setContent {
            val navController = rememberNavController()
            CompositionLocalProvider(LocalNavController provides navController) {
                LaunchedEffect(0) {
                    addSignInListener(navController)
                    onSendAction(navController)
                }
                MaterialTheme(
                    colors = LightColorPalette
                ) {
                    NavHost(navController = navController, startDestination = "conversations") {
                        composable("conversations") { ConversationsView() }
                        composable(
                            "deepLink/{id}",
                            deepLinks = listOf(navDeepLink {
                                uriPattern = "anochat://anochat/conversation/{id}"
                            }),
                        ) {
                            val id = it.arguments?.getString("id")?.toLong()!!
                            navController.navigate("conversation?id=$id") { popUpTo("conversations") }
                        }
                        navigation(startDestination = "conversation", route = "conversationRoute") {
                            composable(
                                "conversation?id={id}",
                                arguments = listOf(
                                    navArgument("id") { nullable = true },
                                    navArgument("uri") { nullable = true }
                                )
                            ) {
                                //if (it.destination.route != navController.currentDestination?.route) return@composable
                                val conversationId = it.arguments?.getString("id")?.toLong()!!
                                val uri = it.arguments?.getString("uri")?.toUri()
                                ConversationView(conversationId, uri)
                            }
                            composable("media") { SendMediaView() }
                        }
                        navigation(startDestination = "settings", route = "settingsRoute") {
                            composable("settings") { SettingsView() }
                            composable("miniature") { MiniatureView() }
                        }
                        composable("user/{id}") {
                            val userId = it.arguments?.getString("id")?.toLong()!!
                            UserView(userId)
                        }
                        composable(
                            "users?uri={uri}",
                            arguments = listOf(navArgument("uri") { nullable = true })
                        ) {
                            val uri = it.arguments?.getString("uri")
                            UsersView(uri)
                        }
                        composable(
                            "image?name={name}&gallery={gallery}",
                            arguments = listOf(
                                navArgument("name") { nullable = true },
                                navArgument("gallery") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                })
                        ) {
                            val image = it.arguments?.getString("name")!!
                            val gallery = it.arguments?.getBoolean("gallery")!!
                            ImageView(image, gallery)
                        }
                        composable("video?uri={uri}") {
                            val uri = it.arguments?.getString("uri")?.toUri()!!
                            VideoView(uri)
                        }
                        composable("login") { SignInView() { this@MainActivity } }
                    }
                }
            }
        }
    }

    private fun onSendAction(navController: NavController) {
        if (intent?.action == Intent.ACTION_SEND) {
            (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                navController.navigate("users?uri=$uri")
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
                "image?name={name}&gallery={gallery}", "video?uri={uri}" -> setFullScreen()
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