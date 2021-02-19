package bogomolov.aa.anochat.features.contacts

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.domain.UserUseCases
import kotlinx.coroutines.runBlocking

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val userUseCases: UserUseCases
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        runBlocking { userUseCases.updateUsersInConversations() }
        return Result.success()
    }
}