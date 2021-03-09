package bogomolov.aa.anochat.features.shared.mvi

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val TAG = "StateLifecycleObserver"

class StateLifecycleObserver<S : UiState>(
    private val updatableView: UpdatableView<S>,
    private val viewModel: BaseViewModel<S>
) : LifecycleObserver {
    private lateinit var updatingJob: Job
    private lateinit var uiState: S

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        uiState = viewModel.createInitialState()
        updatingJob = viewModel.viewModelScope.launch {
            viewModel.stateFlow.collect {
                Log.i(TAG, "${updatableView.javaClass.name} newState:\n${it}\ncurrent:\n$uiState")
                updatableView.updateView(it, uiState)
                uiState = it
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        updatingJob.cancel()
    }
}

interface UpdatableView<S : UiState> {
    fun updateView(newState: S, currentState: S)
}