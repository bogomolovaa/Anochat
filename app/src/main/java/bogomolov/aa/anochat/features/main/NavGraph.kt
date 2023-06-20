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

sealed class Route(val base: String, private val params: String = "") {
    protected val URI = "anochat://anochat/"

    val route: String
        get() = base + params
    val navGraphRoute: String
        get() = "${base}Route"

    object Conversations : Route("conversations")
    object Media : Route("media")
    object Settings : Route("settings")
    object Miniature : Route("miniature")
    object Login : Route("login")

    object Conversation : Route("conversation/{id}", "?uri={uri}") {
        fun route(id: Long, uri: String? = null) = "conversation".let {
            if (uri != null) "$it/$id&uri=$uri" else "$it/$id"
        }

        fun deeplink(id: Long) = "${URI}${route(id)}"
        val deeplink: String
            get() = "${URI}${Conversation.base}"
    }

    object User : Route("user/{id}") {
        fun route(id: Long) = "user/$id"
    }

    object Users : Route("users", "?uri={uri}") {
        fun route(uri: String? = null) = "$base?uri=$uri"
    }

    object Image : Route("image", "?name={name}") {
        fun route(name: String) = "$base?name=$name"
    }

    object Video : Route("video", "?uri={uri}") {
        fun route(video: String) = "$base?uri=$video"
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun NavGraph(getActivity: (() -> Activity)?, onInit: (NavController) -> Unit) {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalNavController provides navController) {
        LaunchedEffect(0) {
            onInit(navController)
        }
        MaterialTheme(
            colors = LightColorPalette
        ) {
            NavHost(navController = navController, startDestination = Route.Conversations.route) {
                composable(Route.Conversations.route) { ConversationsView() }
                navigation(startDestination = Route.Conversation.route, route = Route.Conversation.navGraphRoute) {
                    composable(
                        Route.Conversation.route,
                        arguments = listOf(navArgument("uri") { nullable = true }),
                        deepLinks = listOf(navDeepLink {
                            uriPattern = Route.Conversation.deeplink
                        })
                    ) {
                        val conversationId = it.arguments?.getString("id")?.toLong()!!
                        val uri = it.arguments?.getString("uri")?.toUri()
                        ConversationView(conversationId, uri)
                    }
                    composable(Route.Media.route) { SendMediaView() }
                }
                navigation(startDestination = Route.Settings.route, route = Route.Settings.navGraphRoute) {
                    composable(Route.Settings.route) { SettingsView() }
                    composable(Route.Miniature.route) { MiniatureView() }
                }
                composable(Route.User.route) {
                    val userId = it.arguments?.getString("id")?.toLong()!!
                    UserView(userId)
                }
                composable(
                    Route.Users.route,
                    arguments = listOf(navArgument("uri") { nullable = true })
                ) {
                    val uri = it.arguments?.getString("uri")
                    UsersView(uri)
                }
                composable(Route.Image.route) {
                    val image = it.arguments?.getString("name")!!
                    ImageView(image)
                }
                composable(Route.Video.route) {
                    val uri = it.arguments?.getString("uri")?.toUri()!!
                    VideoView(uri)
                }
                composable(Route.Login.route) { SignInView(getActivity) }
            }
        }
    }
}