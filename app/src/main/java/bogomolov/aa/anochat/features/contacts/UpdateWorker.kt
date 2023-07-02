package bogomolov.aa.anochat.features.contacts

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.domain.UserUseCases
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userUseCases: UserUseCases
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        userUseCases.updateUsersInConversations(true)
        delay(10 * 1000)
        return Result.success()
    }
}