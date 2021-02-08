package bogomolov.aa.anochat.features.shared

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StateLifecycleObserver<S : UiState, V : ViewModel>(
    private val updatableView: UpdatableView<S>,
    private val viewModel: BaseViewModel<S, V>
) : LifecycleObserver {
    private lateinit var updatingJob: Job
    private lateinit var uiState: S

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Log.i("StateLifecycleObserver","onStart()")
        uiState = viewModel.createInitialState()
        updatingJob = viewModel.viewModelScope.launch {
            viewModel.uiState.collect {
                Log.i("StateLifecycleObserver","collect")
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