package bogomolov.aa.anochat.features.contacts

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.repository.repositories.UserRepository
import kotlinx.coroutines.runBlocking

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val userRepository: UserRepository
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        runBlocking { userRepository.updateUsersInConversations() }
        return Result.success()
    }



}