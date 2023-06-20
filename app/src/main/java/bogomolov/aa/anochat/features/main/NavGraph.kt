package bogomolov.aa.anochat.features.main

import android.app.Activity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.net.toUri
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import bogomolov.aa.anochat.features.contacts.list.UsersView
import bogomolov.aa.anochat.features.contacts.user.UserView
import bogomolov.aa.anochat.features.conversations.dialog.ConversationView
import bogomolov.aa.anochat.features.conversations.dialog.SendMediaView
import bogomolov.aa.anochat.features.conversations.list.ConversationsView
import bogomolov.aa.anochat.features.login.SignInView
import bogomolov.aa.anochat.features.settings.MiniatureView
import bogomolov.aa.anochat.features.settings.SettingsView
import bogomolov.aa.anochat.features.shared.ImageView
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.VideoView

val LocalNavController = staticCompositionLocalOf<NavHostController?> { null }

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun NavGraph(getActivity: (() -> Activity)?, onInit: (NavController)->Unit) {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalNavController provides navController) {
        LaunchedEffect(0) {
            onInit(navController)
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
                        "conversation?id={id}&uri={uri}",
                        arguments = listOf(
                            navArgument("id") { nullable = true },
                            navArgument("uri") { nullable = true }
                        )
                    ) {
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
                    "image?name={name}",
                    arguments = listOf(navArgument("name") { nullable = true })
                ) {
                    val image = it.arguments?.getString("name")!!
                    ImageView(image)
                }
                composable("video?uri={uri}") {
                    val uri = it.arguments?.getString("uri")?.toUri()!!
                    VideoView(uri)
                }
                composable("login") { SignInView(getActivity) }
            }
        }
    }
}