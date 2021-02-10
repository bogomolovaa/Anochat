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
        runBlocking {
            val conversations = repository.loadAllConversations()
            for (conversation in conversations) {
                val user = repository.receiveUser(conversation.user.uid)
                if (user != null) repository.syncFromRemoteUser(user, saveLocal = true, loadFullPhoto = true)
            }
        }
        return Result.success()
    }

}