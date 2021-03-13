package bogomolov.aa.anochat.repository

import android.R.attr.bitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import bogomolov.aa.anochat.features.shared.*
import dagger.hilt.android.qualifiers.ApplicationContext
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
        val fileName = "${getRandomFileName()}.jpg"
        try {
            var resizedBitmap = Bitmap.createScaledBitmap(fastResizedBitmap!!, width, height, true)
            if (path != null) resizedBitmap = rotateIfNeeded(resizedBitmap, path)
            if (toGallery && authRepository.getSettings().gallery) {
                saveImageToGallery(resizedBitmap, fileName)
            } else {
                saveImageToPath(resizedBitmap, fileName)
            }
            return BitmapWithName(fileName, resizedBitmap)
        } catch (e: Exception) {
            Log.w(TAG, "resizeImage", e)
        }
        return null
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