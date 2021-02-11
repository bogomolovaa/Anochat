package bogomolov.aa.anochat.features.contacts

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.runBlocking

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    val repository: Repository
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        runBlocking { repository.updateUsersInConversations() }
        return Result.success()
    }



}