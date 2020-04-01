package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import bogomolov.aa.anochat.AnochatAplication
import bogomolov.aa.anochat.android.UpdateWorker
import bogomolov.aa.anochat.dagger.MyWorkerFactory
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivityViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    suspend fun isSignedIn() = repository.isSignedIn()

    fun startWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        initializeWorkManager().enqueueUniquePeriodicWork(
            "updateUsers",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWorkRequest
        )
    }

    fun setOnline(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.setOnline()
        }
    }

    fun setOffline(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.setOffline()
        }
    }

    private fun initializeWorkManager(): WorkManager {
        val appContext = repository.getContext() as AnochatAplication
        val factory = appContext.workManagerConfiguration.workerFactory
                as DelegatingWorkerFactory
        factory.addFactory(MyWorkerFactory(repository))

        return WorkManager.getInstance(appContext)
    }
}