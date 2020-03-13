package bogomolov.aa.anochat.android

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UpdateWorker(
    val repository: Repository,
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        GlobalScope.launch {
            val conversations = repository.loadConversations()
            for (conversation in conversations) {
                val user = repository.receiveUser(conversation.user.uid)
                if (user != null) repository.updateUserFrom(user)
            }
        }

        return Result.success()
    }

}