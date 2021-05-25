package bogomolov.aa.anochat.features.shared

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.*

private const val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz1234567890"
private const val GALLERY_FOLDER = "Anochat"
private const val TAG = "FileUtils"

data class BitmapWithName(val name: String, val bitmap: Bitmap?){
    var processed: Boolean = true
    var progress = MutableSharedFlow<Int>(0)
}

fun getMiniPhotoFileName(fileName: String) = File(fileName).nameWithoutExtension + "_mini.jpg"

fun getFilePath(context: Context, fileName: String) = File(getFilesDir(context), fileName).path

fun nameToImage(name: String) = "${name}.jpg"

fun nameToVideo(name: String) = "${name}.mp4"

fun videoThumbnail(video: String) = nameToImage(video.substringBefore("."))

private fun getFilesDir(context: Context): File = context.filesDir

fun getRandomFileName() = (1..20).map { allowedChars.random() }.joinToString(separator = "")

fun getBitmap(fileName: String?, context: Context, quality: Int = 1): Bitmap? {
    if (fileName == null) return null
    val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = quality }
    return BitmapFactory.decodeFile(getFilePath(context, fileName), bitmapOptions)
}

fun getBitmapFromGallery(fileName: String?, context: Context, quality: Int = 1): Bitmap? {
    if (fileName == null) return null
    val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = quality }
    var inputStream = getGalleryInputStream(fileName, context)
    if (inputStream == null) inputStream = getFileInputStream(fileName, context)
    return BitmapFactory.decodeStream(inputStream, null, bitmapOptions)
        .also { inputStream?.close() }
}

fun getGalleryInputStream(fileName: String, context: Context): InputStream? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getGalleryUri(fileName, context)?.let { context.contentResolver.openInputStream(it) }
    } else {
        when {
            fileName.endsWith(".jpg") -> DIRECTORY_PICTURES
            fileName.endsWith(".mp4") -> DIRECTORY_MOVIES
            else -> null
        }?.let {
            val imagesDir =
                File("${Environment.getExternalStoragePublicDirectory(it)}/$GALLERY_FOLDER")
            return try {
                FileInputStream(File(imagesDir, fileName).path)
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "getGalleryInputStream exception: ${e.message}")
                null
            }
        }
    }

fun getUri(fileName: String, context: Context): Uri? {
    return getGalleryUri(fileName, context) ?: getFileProviderUri(fileName, context)
}

fun getUriWithSource(fileName: String, context: Context): UriWithSource {
    var fromGallery = true
    val uri = getGalleryUri(fileName, context) ?: run {
        fromGallery = false
        getFileProviderUri(fileName, context)
    }
    return UriWithSource(uri, fromGallery)
}

data class UriWithSource(val uri: Uri?, val fromGallery: Boolean)

fun getFileProviderUri(fileName: String, context: Context): Uri? =
    try {
        FileProvider.getUriForFile(
            context,
            "bogomolov.aa.anochat.fileprovider",
            File(getFilePath(context, fileName))
        )
    } catch (e: java.lang.Exception) {
        Toast.makeText(context, "${e.message}", LENGTH_LONG).show()
        null
    }

fun getGalleryOutputStream(fileName: String, context: Context): OutputStream? {
    if (fileName.endsWith(".jpg")) return getGalleryOutputStreamImage(fileName, context)
    if (fileName.endsWith(".mp4")) return getGalleryOutputStreamVideo(fileName, context)
    return null
}

private fun getGalleryOutputStreamImage(fileName: String, context: Context): OutputStream? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val imagesPath = "${DIRECTORY_PICTURES}/$GALLERY_FOLDER"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.RELATIVE_PATH, imagesPath)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        return try {
            val uri =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) context.contentResolver.openOutputStream(uri) else null
        } catch (e: SecurityException) {
            Log.w(TAG, "getGalleryOutputStream exception: ${e.message}")
            null
        }
    } else {
        val imagesPath =
            "${Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)}/$GALLERY_FOLDER"
        val directory = File(imagesPath)
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, fileName)
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.DATA, file.absolutePath)
        return try {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            FileOutputStream(file)
        } catch (e: SecurityException) {
            Log.w(TAG, "getGalleryOutputStream exception: ${e.message}")
            null
        }
    }
}

private fun getGalleryOutputStreamVideo(fileName: String, context: Context): OutputStream? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val imagesPath = "${DIRECTORY_MOVIES}/$GALLERY_FOLDER"
        val values = ContentValues()
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/*")
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.RELATIVE_PATH, imagesPath)
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
        return try {
            val uri =
                context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) context.contentResolver.openOutputStream(uri) else null
        } catch (e: SecurityException) {
            Log.w(TAG, "getGalleryOutputStream exception: ${e.message}")
            null
        }
    } else {
        val imagesPath =
            "${Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES)}/$GALLERY_FOLDER"
        val directory = File(imagesPath)
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, fileName)
        val values = ContentValues()
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/*")
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.DATA, file.absolutePath)
        return try {
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            FileOutputStream(file)
        } catch (e: SecurityException) {
            Log.w(TAG, "getGalleryOutputStream exception: ${e.message}")
            null
        }
    }
}

fun getFileInputStream(fileName: String, context: Context): InputStream? {
    return try {
        FileInputStream(File(getFilePath(context, fileName)))
    } catch (e: FileNotFoundException) {
        Log.w(TAG, "getFileInputStream ${e.message}")
        null
    }
}

fun getGalleryUri(fileName: String, context: Context): Uri? {
    if (fileName.endsWith(".jpg")) return getGalleryUriImage(fileName, context)
    if (fileName.endsWith(".mp4")) return getGalleryUriVideo(fileName, context)
    return null
}

private fun getGalleryUriImage(fileName: String, context: Context): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection =
        "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(fileName)
    try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use {
            if (it.moveToNext()) {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    it.getLong(it.getColumnIndex(MediaStore.Images.Media._ID))
                )
                return uri
            } else {
                Log.w(TAG, "getGalleryInputStream $fileName not found")
            }
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "getGalleryInputStream exception: ${e.message}")
    }
    return null
}

private fun getGalleryUriVideo(fileName: String, context: Context): Uri? {
    val projection = arrayOf(MediaStore.Video.Media._ID)
    val selection =
        "${MediaStore.Video.Media.DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(fileName)
    try {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use {
            if (it.moveToNext()) {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    it.getLong(it.getColumnIndex(MediaStore.Video.Media._ID))
                )
                return uri
            } else {
                Log.w(TAG, "getGalleryInputStream $fileName not found")
            }
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "getGalleryInputStream exception: ${e.message}")
    }
    return null
}