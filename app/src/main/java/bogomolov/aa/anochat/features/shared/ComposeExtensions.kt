package bogomolov.aa.anochat.features.shared

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T> EventHandler(uiEvents: Flow<T>, eventCollector: suspend (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiEventsLifecycleAware = remember(uiEvents, lifecycleOwner) {
        uiEvents.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(uiEventsLifecycleAware, eventCollector) {
        uiEventsLifecycleAware.collect(eventCollector)
    }
}

@Composable
fun <T : Any> collectState(flow: StateFlow<T>, content: @Composable (T) -> Unit) {
    content(flow.collectAsState().value)
}

@Immutable
class ImmutableList<T>(list: List<T>): List<T> by list

fun <T> List<T>.asImmutableList() = ImmutableList(this)

@Immutable
class ImmutableFlow<T>(flow: Flow<T>): Flow<T> by flow

fun <T> Flow<T>.asImmutableFlow() = ImmutableFlow(this)
