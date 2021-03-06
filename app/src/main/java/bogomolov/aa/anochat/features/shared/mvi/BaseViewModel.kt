package bogomolov.aa.anochat.features.shared.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface Event

abstract class BaseViewModel<S : Any>(
    val initialState: S,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state: MutableStateFlow<S> = MutableStateFlow(initialState)
    val state = _state.asStateFlow()
    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    private val mutex = Mutex()
    val currentState get() = state.value

    protected fun execute(run: suspend () -> Unit) {
        viewModelScope.launch(dispatcher) { run() }
    }

    fun updateState(reduce: S.() -> S) {
        viewModelScope.launch(dispatcher) {
            setState(reduce)
        }
    }

    fun updateStateBlocking(reduce: S.() -> S) = runBlocking {
        setState(reduce)
    }

    protected suspend fun addEvent(event: Event) {
        _events.send(event)
    }

    protected suspend fun setState(reduce: S.() -> S) {
        mutex.withLock { _state.value = _state.value.reduce().also { println("setState $it") } }
    }
}