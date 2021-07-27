package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import bogomolov.aa.anochat.features.shared.mvi.Event
import kotlinx.coroutines.flow.Flow

@SuppressLint("ComposableNaming")
@Composable
fun Flow<Event>.collect(block: (Event) -> Unit) {
    val events = this
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationFlowLifecycleAware = remember(events, lifecycleOwner) {
        events.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
    }
    val event by locationFlowLifecycleAware.collectAsState(null)
    event?.let {
        block(it)
    }
}