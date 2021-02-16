package bogomolov.aa.anochat.dagger

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.features.contacts.UpdateWorker

class MyWorkerFactory(
    private val userUseCases: UserUseCases
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when (workerClassName) {
            UpdateWorker::class.java.name ->
                UpdateWorker(appContext, workerParameters, userUseCases)
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }
    }
}