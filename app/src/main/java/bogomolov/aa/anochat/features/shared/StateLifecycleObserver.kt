package bogomolov.aa.anochat.features.shared

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StateLifecycleObserver<S : UiState, V : ActionContext>(
    private val updatableView: UpdatableView<S>,
    private val viewModel: BaseViewModel<S, V>
) : LifecycleObserver {
    private lateinit var updatingJob: Job
    private lateinit var uiState: S

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        uiState = viewModel.createInitialState()
        updatingJob = viewModel.viewModelScope.launch {
            viewModel.uiState.collect {
                Log.i(
                    "StateLifecycleObserver",
                    "${updatableView.javaClass.name} updateView newState:\n${it}\ncurrentState:\n$uiState"
                )
                updatableView.updateView(it, uiState)
                uiState = it
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            updatingJob.cancel()
        }
    }
}