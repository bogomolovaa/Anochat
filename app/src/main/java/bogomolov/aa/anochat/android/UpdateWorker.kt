package bogomolov.aa.anochat.android

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

public class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    val repository: Repository
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        runBlocking {
            Log.i("test","start UpdateWorker")
            val conversations = repository.loadConversations()
            for (conversation in conversations) {
                val user = repository.receiveUser(conversation.user.uid)
                if (user != null) repository.updateUserFrom(user)
            }
        }
        return Result.success()
    }

}