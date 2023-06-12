package bogomolov.aa.anochat.features.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow

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