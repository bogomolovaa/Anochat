package bogomolov.aa.anochat.features.main

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
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
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

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
            if (uri != null) "$it/$id?uri=$uri" else "$it/$id"
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun NavGraph(getActivity: (() -> Activity)?, onInit: (NavController) -> Unit) {
    val navController = rememberAnimatedNavController()
    val defaultEnterTransition = fadeIn(animationSpec = tween(100))
    val defaultExitTransition = fadeOut(animationSpec = tween(100))
    CompositionLocalProvider(LocalNavController provides navController) {
        LaunchedEffect(0) {
            onInit(navController)
        }
        MaterialTheme(
            colors = LightColorPalette
        ) {
            AnimatedNavHost(
                navController = navController,
                startDestination = Route.Conversations.route,
                enterTransition = { defaultEnterTransition },
                exitTransition = { defaultExitTransition }) {
                composable(Route.Conversations.route) { ConversationsView() }
                navigation(startDestination = Route.Conversation.route, route = Route.Conversation.navGraphRoute) {
                    composable(
                        Route.Conversation.route,
                        arguments = listOf(navArgument("uri") { nullable = true }),
                        deepLinks = listOf(navDeepLink {
                            uriPattern = Route.Conversation.deeplink
                        }),
                        enterTransition = {
                            if (initialState.destination.route == Route.Conversations.route) {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(300)
                                )
                            } else defaultEnterTransition
                        },
                        exitTransition = {
                            if (targetState.destination.route == Route.Conversations.route) {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(300)
                                )
                            } else defaultExitTransition
                        }
                    ) {
                        val conversationId = it.arguments?.getString("id")?.toLong()!!
                        val uri = it.arguments?.getString("uri")
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
                    it.arguments?.getString("uri")?.let { VideoView(it) }
                }
                composable(Route.Login.route) { SignInView(getActivity) }
            }
        }
    }
}