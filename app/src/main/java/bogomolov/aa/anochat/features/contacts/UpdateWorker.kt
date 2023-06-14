package bogomolov.aa.anochat.features.contacts

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.domain.UserUseCases
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userUseCases: UserUseCases
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        runBlocking {
            userUseCases.updateUsersInConversations(true)
            delay(10 * 1000)
        }
        return Result.success()
    }
}