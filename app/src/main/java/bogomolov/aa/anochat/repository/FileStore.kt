package bogomolov.aa.anochat.repository

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import bogomolov.aa.anochat.features.shared.*
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min


private const val TAG = "FileStoreImpl"
private const val MAX_IMAGE_DIM = 1024f

interface FileStore {
    fun saveByteArray(byteArray: ByteArray, fileName: String, toGallery: Boolean)
    fun getByteArray(fromGallery: Boolean, fileName: String): ByteArray?
    fun fileExists(fileName: String): Boolean
    fun resizeImage(
        uri: Uri? = null,
        path: String? = null,
        toGallery: Boolean
    ): BitmapWithName?

    fun resizeVideo(uri: Uri, coroutineScope: CoroutineScope): BitmapWithName?
    fun createVideoThumbnail(videoName: String)
}

@Singleton
class FileStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : FileStore {

    override fun saveByteArray(byteArray: ByteArray, fileName: String, toGallery: Boolean) {
        if (toGallery && authRepository.getSettings().gallery)
            try {
                val outputStream = getGalleryOutputStream(fileName, context)
                if (outputStream != null) {
                    outputStream.write(byteArray)
                    outputStream.flush()
                    outputStream.close()
                    return
                }
            } catch (e: IOException) {
                Log.w(TAG, "saveByteArray", e)
            }
        File(getFilePath(context, fileName)).writeBytes(byteArray)
    }

    override fun getByteArray(fromGallery: Boolean, fileName: String): ByteArray? {
        var inputStream = if (fromGallery) getGalleryInputStream(fileName, context) else null
        if (inputStream == null) inputStream = getFileInputStream(fileName, context)
        return inputStream?.readBytes()?.also { inputStream.close() }
    }

    override fun fileExists(fileName: String) = File(getFilePath(context, fileName)).exists()

    override fun resizeVideo(uri: Uri, coroutineScope: CoroutineScope): BitmapWithName? {
        val size = (getRealSizeFromUri(context, uri)?.toFloat() ?: 0f) / 1024 / 1024
        if (size > 200) {
            Toast.makeText(context, "Too large file", Toast.LENGTH_LONG).show()
            return null
        }
        val videoName = getRandomFileName()
        val originalVideoFile = File(getFilePath(context, nameToVideo(videoName + "_")))
        val videoFile = File(getFilePath(context, nameToVideo(videoName)))

        saveUriToFile(uri, originalVideoFile)
        val thumbnailBitmap = createVideoThumbnail(originalVideoFile)
        if (thumbnailBitmap != null) {
            saveImageToPath(thumbnailBitmap, nameToImage(videoName))
            val result = BitmapWithName(videoName, thumbnailBitmap).apply { processed = false }
            coroutineScope.launch(Dispatchers.Default) {
                val mediaPlayer = MediaPlayer.create(context, uri)
                val videoLength = mediaPlayer.duration
                mediaPlayer.release()
                if (videoLength > 0) {
                    Config.resetStatistics()
                    Config.enableStatisticsCallback { statistics ->
                        if (!isActive)
                            GlobalScope.launch(Dispatchers.Default) {
                                FFmpeg.cancel()
                            }
                        coroutineScope.launch(Dispatchers.Default) {
                            val progress = statistics.time.toFloat() / videoLength
                            result.progress.emit((100 * progress).toInt())
                        }
                    }
                }
                compressVideo(originalVideoFile.absolutePath, videoFile.absolutePath)
                originalVideoFile.delete()
                result.processed = true
            }
            return result
        }
        return null
    }

    private fun compressVideo(inputPath: String, outputPath: String) {
        //val rc = FFmpeg.execute("-i $inputPath -b 800k $outputPath")
        val rc = FFmpeg.execute("-i $inputPath -vcodec libx264 -crf 24 $outputPath")

        if (rc == RETURN_CODE_SUCCESS) {
            Log.i(Config.TAG, "Command execution completed successfully.")
        } else if (rc == RETURN_CODE_CANCEL) {
            Log.i(Config.TAG, "Command execution cancelled by user.")
        } else {
            Log.i(
                Config.TAG,
                String.format("Command execution failed with rc=%d and the output below.", rc)
            )
            Config.printLastCommandOutput(Log.INFO)
        }
    }

    override fun createVideoThumbnail(videoName: String) {
        getUri(videoName, context)?.let { uri ->
            val tempVideoFile = File(getFilePath(context, "temp_$videoName"))
            saveUriToFile(uri, tempVideoFile)
            createVideoThumbnail(tempVideoFile)?.let { saveImageToPath(it, videoThumbnail(videoName)) }
            tempVideoFile.delete()
        }
    }

    private fun getRealSizeFromUri(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {

            val proj = arrayOf(MediaStore.Audio.Media.SIZE)
            cursor = context.contentResolver.query(uri, proj, null, null, null)
            cursor?.let {
                val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                cursor.moveToFirst()
                cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
    }

    override fun resizeImage(
        uri: Uri?,
        path: String?,
        toGallery: Boolean
    ): BitmapWithName? {
        val dimensionsBitmapOptions = getImageDimensions(uri, path, context)
        val bitmapWidth = dimensionsBitmapOptions.outWidth
        val bitmapHeight = dimensionsBitmapOptions.outHeight

        val ratio = min(MAX_IMAGE_DIM / max(bitmapWidth, bitmapHeight), 1.0f)
        val quality = (1 / ratio).toInt()
        val fastResizedBitmap = loadBitmap(uri, path, context, quality)

        val width = (ratio * bitmapWidth).toInt()
        val height = (ratio * bitmapHeight).toInt()
        val name = getRandomFileName()
        val fileName = nameToImage(name)
        try {
            var resizedBitmap = Bitmap.createScaledBitmap(fastResizedBitmap!!, width, height, true)
            if (path != null) resizedBitmap = rotateIfNeeded(resizedBitmap, path)
            if (toGallery && authRepository.getSettings().gallery) {
                saveImageToGallery(resizedBitmap, fileName)
            } else {
                saveImageToPath(resizedBitmap, fileName)
            }
            return BitmapWithName(name, resizedBitmap)
        } catch (e: Exception) {
            Log.w(TAG, "resizeImage", e)
        }
        return null
    }

    private fun saveUriToFile(uri: Uri, file: File) {
        context.contentResolver.openInputStream(uri)?.copyTo(file.outputStream())
    }

    private fun createVideoThumbnail(videoFile: File): Bitmap? =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ThumbnailUtils.createVideoThumbnail(videoFile, Size(500, 500), null)
        } else {
            ThumbnailUtils.createVideoThumbnail(
                videoFile.path,
                MediaStore.Video.Thumbnails.MINI_KIND
            )
        }

    private fun rotateIfNeeded(bitmap: Bitmap, photoPath: String): Bitmap? {
        val orientation = ExifInterface(photoPath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> bitmap
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float) =
        Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            Matrix().apply { postRotate(angle) }, true
        )

    private fun getImageDimensions(
        uri: Uri? = null,
        path: String? = null,
        context: Context
    ): BitmapFactory.Options {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inJustDecodeBounds = true

        if (uri != null) {
            val ins = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(ins, null, bitmapOptions)
            ins?.close()
        } else if (path != null) {
            BitmapFactory.decodeFile(path, bitmapOptions)
        }
        return bitmapOptions
    }

    private fun loadBitmap(
        uri: Uri? = null,
        path: String? = null,
        context: Context,
        quality: Int = 1
    ): Bitmap? {
        val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = quality }
        return if (uri != null) {
            val ins2 = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(ins2, null, bitmapOptions).also { ins2?.close() }
        } else {
            BitmapFactory.decodeFile(path, bitmapOptions)
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, fileName: String) {
        val outputStream = getGalleryOutputStream(fileName, context)
        if (outputStream != null) {
            saveImageToStream(bitmap, outputStream)
        } else {
            saveImageToPath(bitmap, fileName)
        }
    }

    private fun saveImageToPath(bitmap: Bitmap, fileName: String) {
        val filePath = getFilePath(context, fileName)
        saveImageToStream(bitmap, FileOutputStream(filePath))
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream) {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()
    }
}