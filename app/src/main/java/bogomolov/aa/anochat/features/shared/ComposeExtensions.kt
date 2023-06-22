package bogomolov.aa.anochat.features.shared

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

@Composable
fun <T> Flow<T>.collectEvents(eventCollector: suspend (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiEventsLifecycleAware = remember(this, lifecycleOwner) {
        this.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    LaunchedEffect(uiEventsLifecycleAware, eventCollector) {
        uiEventsLifecycleAware.collect(eventCollector)
    }
}

@Composable
fun <T> StateFlow<T>.collectState(content: @Composable (T) -> Unit) {
    content(collectAsStateWithLifecycle().value)
}

val InsetsModifier by lazy { Modifier.statusBarsPadding().navigationBarsPadding().imePadding() }

data class KeyboardState(
    val opened: Boolean = false,
    val height: Int = 240
)

@Composable
fun keyboardAsState(onChange: (Boolean) -> Unit = {}): State<KeyboardState> {
    val keyboardState = remember { mutableStateOf(KeyboardState()) }
    val view = LocalView.current
    val density = LocalDensity.current.density
    LaunchedEffect(view) {
        var height = KeyboardState().height
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val opened = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (opened != keyboardState.value.opened) onChange(opened)
            val keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navbarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val newHeight = ((keyboardHeight - navbarHeight) / density).toInt()
            height = max(height, newHeight)
            keyboardState.value = KeyboardState(opened, height)
            insets
        }
    }
    return keyboardState
}

@Immutable
class ImmutableList<T>(list: List<T>) : List<T> by list

fun <T> List<T>.asImmutableList() = ImmutableList(this)

@Immutable
class ImmutableFlow<T>(flow: Flow<T>) : Flow<T> by flow

fun <T> Flow<T>.asImmutableFlow() = ImmutableFlow(this)
