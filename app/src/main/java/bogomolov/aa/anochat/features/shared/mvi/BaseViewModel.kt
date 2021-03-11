package bogomolov.aa.anochat.features.shared.mvi

import android.util.Log
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

interface UiState
interface UserAction

abstract class BaseViewModel<S : UiState> : ViewModel(), ActionExecutor {
    private val mutex = Mutex()

    var dispatcher: CoroutineDispatcher = Dispatchers.IO

    val state: S
        get() = stateFlow.value

    private val initialState: S by lazy { createInitialState() }
    abstract fun createInitialState(): S

    private val _uiState: MutableStateFlow<S> = MutableStateFlow(initialState)
    val stateFlow = _uiState.asStateFlow()

    private val _actions = Channel<UserAction>()
    private val actions = _actions.receiveAsFlow()
    private var subscribed = false
    private var actionListener: ActionListener? = null
    private var lastAction: UserAction? = null

    fun addActionListener(actionListener: ActionListener): UserAction? {
        this.actionListener = actionListener
        return lastAction
    }

    private fun subscribeToActions() {
        subscribed = true
        viewModelScope.launch(dispatcher) {
            actions.collect {
                handleAction(it)
            }
        }
    }

    protected abstract suspend fun handleAction(action: UserAction)

    override fun addAction(action: UserAction) {
        Log.d("BaseViewModel", "addAction ${action.javaClass.simpleName}")
        actionListener?.onAction(action)
        lastAction = action
        viewModelScope.launch(dispatcher) {
            mutex.withLock { if (!subscribed) subscribeToActions() }
            _actions.send(action)
        }
    }

    fun setStateBlocking(reduce: S.() -> S) {
        runBlocking {
            setState(reduce)
        }
    }

    suspend fun setState(reduce: S.() -> S) {
        mutex.withLock {
            val newState = state.reduce()
            _uiState.value = newState
        }
    }

    fun setStateAsync(reduce: S.() -> S) {
        viewModelScope.launch(dispatcher) {
            setState(reduce)
        }
    }
}

interface ActionExecutor {
    fun addAction(action: UserAction)
}

fun interface ActionListener {
    fun onAction(userAction: UserAction)
}