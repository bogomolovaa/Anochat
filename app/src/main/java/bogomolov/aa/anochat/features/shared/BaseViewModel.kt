package bogomolov.aa.anochat.features.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.features.login.SignInUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class BaseViewModel<S : UiState, V : ViewModel> : ViewModel() {
    private val mutex = Mutex()

    val currentState: S
        get() = uiState.value

    private val initialState: S by lazy { createInitialState() }
    abstract fun createInitialState(): S

    private val _uiState: MutableStateFlow<S> = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _actions = Channel<UserAction<V>>()
    private val actions = _actions.receiveAsFlow()

    init {
        subscribeEvents()
    }

    fun addAction(action: UserAction<V>) {
        viewModelScope.launch(Dispatchers.IO) { _actions.send(action) }
    }

    private fun subscribeEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            actions.collect {
                handleAction(it)
            }
        }
    }

    protected open suspend fun handleAction(action: UserAction<V>) {
        action.execute(this as V)
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

interface UiState {}
interface UserAction<V : ViewModel> {
    suspend fun execute(viewModel: V)
}

interface UpdatableView<S : UiState>{
    fun updateView(newState: S, currentState: S)
}