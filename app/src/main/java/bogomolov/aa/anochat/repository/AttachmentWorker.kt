package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import bogomolov.aa.anochat.domain.Crypto
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "AttachmentWorker"

@HiltWorker
class AttachmentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val crypto: Crypto,
    private val notificationService: NotificationsService,
    private val messageRep: MessageRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val fileName = inputData.getString(FILE_NAME)?: return Result.failure()
        val uid = inputData.getString(UID) ?: return Result.failure()
        crypto.getSecretKey(uid)?.let {
            Log.d(TAG, "sendAttachment $fileName to uid $uid")
            messageRep.sendAttachment(fileName, uid) { crypto.encrypt(it, this) }
        } ?: return Result.failure()
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, notificationService.createAttachmentForegroundNotification())
    }

    companion object{
        const val FILE_NAME = "filename"
        const val UID = "uid"
        private const val NOTIFICATION_ID = 2
    }
}