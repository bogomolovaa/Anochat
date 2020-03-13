package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.work.*
import bogomolov.aa.anochat.android.UpdateWorker
import bogomolov.aa.anochat.repository.Repository
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

        WorkManager.getInstance(repository.getContext()).enqueueUniquePeriodicWork(
            "updateUsers",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWorkRequest
        )
    }
}