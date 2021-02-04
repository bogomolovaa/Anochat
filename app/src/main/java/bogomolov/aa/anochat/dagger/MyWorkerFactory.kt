package bogomolov.aa.anochat.dagger

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.features.contacts.UpdateWorker
import bogomolov.aa.anochat.repository.Repository

class MyWorkerFactory(
    private val repository: Repository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when (workerClassName) {
            UpdateWorker::class.java.name ->
                UpdateWorker(appContext, workerParameters, repository)
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }
    }
}