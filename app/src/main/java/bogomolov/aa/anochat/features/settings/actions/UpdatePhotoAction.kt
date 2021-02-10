package bogomolov.aa.anochat.features.settings.actions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import bogomolov.aa.anochat.features.settings.SettingsUiState
import bogomolov.aa.anochat.features.shared.DefaultContext
import bogomolov.aa.anochat.features.shared.DefaultUserAction
import bogomolov.aa.anochat.repository.getFilePath
import bogomolov.aa.anochat.repository.getMiniPhotoFileName
import java.io.FileOutputStream

class UpdatePhotoAction(
    val photo: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) : DefaultUserAction<SettingsUiState>() {

    override suspend fun execute(context: DefaultContext<SettingsUiState>) {
        val user = context.viewModel.currentState.user
        if (user != null) {
            val filePath = getFilePath(context.repository.getContext(), photo)
            val bitmap = BitmapFactory.decodeFile(filePath)
            val miniBitmap = Bitmap.createBitmap(
                bitmap,
                Math.max(x, 0),
                Math.max(y, 0),
                Math.min(bitmap.width - Math.max(x, 0), width),
                Math.min(bitmap.height - Math.max(y, 0), height)
            )
            val userWithNewPhoto = user.copy(photo = photo)
            context.viewModel.setState { copy(user = userWithNewPhoto) }
            context.repository.updateMyUser(userWithNewPhoto)
            val appContext = context.repository.getContext()
            val miniPhotoPath = getFilePath(appContext, getMiniPhotoFileName(appContext, photo))
            miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))
        }
    }
}