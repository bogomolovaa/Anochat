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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface UiState
interface UserAction

abstract class BaseViewModel<S : UiState> : ViewModel() {
    private val mutex = Mutex()

    var dispatcher: CoroutineDispatcher = Dispatchers.IO

    val currentState: S
        get() = uiState.value

    private val initialState: S by lazy { createInitialState() }
    abstract fun createInitialState(): S

    private val _uiState: MutableStateFlow<S> = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _actions = Channel<UserAction>()
    private val actions = _actions.receiveAsFlow()
    private var subscribed = false


    private fun subscribeToActions() {
        subscribed = true
        viewModelScope.launch(dispatcher) {
            actions.collect {
                handleAction(it)
            }
        }
    }

    protected abstract suspend fun handleAction(action: UserAction)

    fun addAction(action: UserAction) {
        viewModelScope.launch(dispatcher) {
            mutex.withLock { if (!subscribed) subscribeToActions() }
            _actions.send(action)
        }
    }

    suspend fun setState(reduce: S.() -> S) {
        mutex.withLock {
            val newState = currentState.reduce()
            _uiState.value = newState
        }
    }

    fun setStateAsync(reduce: S.() -> S) {
        viewModelScope.launch(dispatcher) {
            setState(reduce)
        }
    }

}
