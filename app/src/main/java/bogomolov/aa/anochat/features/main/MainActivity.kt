package bogomolov.aa.anochat.features.main

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.net.toUri
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.work.*
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
import bogomolov.aa.anochat.features.shared.VideoView
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

    @ExperimentalMaterialApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //emojiSupport()
        startWorkManager()

        setContent {
            val navController = rememberNavController()
            LaunchedEffect(0) {
                addSignInListener(navController)
                onSendAction(navController)
            }
            NavHost(navController = navController, startDestination = "conversations") {
                composable("conversations") { ConversationsView(navController) }
                composable(
                    "deepLink/{id}",
                    deepLinks = listOf(navDeepLink { uriPattern = "anochat://anochat/conversation/{id}" }),
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
                        ConversationView(navController, conversationId, uri)
                    }
                    composable("media") { SendMediaView(navController) }
                }
                navigation(startDestination = "settings", route = "settingsRoute") {
                    composable("settings") { SettingsView(navController) }
                    composable("miniature") { MiniatureView(navController) }
                }
                composable("user/{id}") {
                    val userId = it.arguments?.getString("id")?.toLong()!!
                    UserView(userId, navController)
                }
                composable(
                    "users?uri={uri}",
                    arguments = listOf(navArgument("uri") { nullable = true })
                ) {
                    val uri = it.arguments?.getString("uri")
                    UsersView(uri, navController)
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
                    ImageView(image, gallery, navController)
                }
                composable("video?uri={uri}") {
                    val uri = it.arguments?.getString("uri")?.toUri()!!
                    VideoView(uri)
                }
                composable("login") { SignInView(navController) { this@MainActivity } }
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

    override fun onStart() {
        super.onStart()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    private fun addSignInListener(navController: NavController) {
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            if (destination.route != "login" && !authRepository.isSignedIn()) navigateToSignIn(controller)
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
}