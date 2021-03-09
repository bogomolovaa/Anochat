package bogomolov.aa.anochat.features.shared.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.test.espresso.idling.CountingIdlingResource

interface UiState
interface UserAction

abstract class BaseViewModel<S : UiState> : ViewModel(), ActionExecutor {
    private val mutex = Mutex()

    var dispatcher: CoroutineDispatcher = Dispatchers.IO
    //val idlingResource = CountingIdlingResource("BaseViewModel IdlingResource")

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

    fun addActionListener(actionListener: ActionListener){
        this.actionListener = actionListener
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
        actionListener?.onAction(action)
        viewModelScope.launch(dispatcher) {
            mutex.withLock { if (!subscribed) subscribeToActions() }
            _actions.send(action)
        }
    }

    suspend fun setState(reduce: S.() -> S) {
        mutex.withLock {
            val newState = state.reduce()
            _uiState.value = newState
        }
    }

    fun setStateAsync(reduce: S.() -> S) {
        //idlingResource.increment()
        viewModelScope.launch(dispatcher) {
            delay(3000)
            setState(reduce)
            //idlingResource.decrement()
        }
    }

}

interface ActionExecutor {
    fun addAction(action: UserAction)
}

fun interface ActionListener{
    fun onAction(userAction: UserAction)
}