package bogomolov.aa.anochat.features.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
interface ActionContext
interface UserAction<C : ActionContext> {
    suspend fun execute(context: C)
}

abstract class BaseViewModel<S : UiState, C : ActionContext> : ViewModel() {
    private val mutex = Mutex()

    protected val viewModelContext: C by lazy { createViewModelContext() }

    protected abstract fun createViewModelContext(): C

    val currentState: S
        get() = uiState.value

    private val initialState: S by lazy { createInitialState() }
    abstract fun createInitialState(): S

    private val _uiState: MutableStateFlow<S> = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _actions = Channel<UserAction<C>>()
    private val actions = _actions.receiveAsFlow()

    init {
        subscribeEvents()
    }

    fun addAction(action: UserAction<C>) {
        viewModelScope.launch(Dispatchers.IO) { _actions.send(action) }
    }

    private fun subscribeEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            actions.collect {
                handleAction(it)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected open suspend fun handleAction(action: UserAction<C>) {
        action.execute(viewModelContext as C)
    }

    suspend fun setState(reduce: S.() -> S) {
        mutex.withLock {
            val newState = currentState.reduce()
            _uiState.value = newState
        }
    }

    fun setStateAsync(reduce: S.() -> S) {
        viewModelScope.launch(Dispatchers.IO) {
            setState(reduce)
        }
    }

}
