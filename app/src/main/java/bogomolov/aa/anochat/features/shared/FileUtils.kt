package bogomolov.aa.anochat.features.shared

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.Log
import java.io.*
import kotlin.math.max
import kotlin.math.min

private const val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz1234567890"
private const val MAX_IMAGE_DIM = 1024f
private const val GALLERY_FOLDER = "Anochat"
private const val TAG = "FileUtils"

data class BitmapWithName(val name: String, val bitmap: Bitmap)

fun getMiniPhotoFileName(fileName: String) = File(fileName).nameWithoutExtension + "_mini.jpg"

fun getFilePath(context: Context, fileName: String) = File(getFilesDir(context), fileName).path

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

fun ByteArray.save(toGallery: Boolean, fileName: String, context: Context) {
    if (toGallery)
        try {
            val outputStream = getGalleryOutputStream(fileName, context)
            if (outputStream != null) {
                outputStream.write(this)
                outputStream.flush()
                outputStream.close()
                return
            }
        } catch (e: IOException) {
            Log.w(TAG, "saveByteArray", e)
        }
    File(getFilePath(context, fileName)).writeBytes(this)
}

fun getByteArray(fromGallery: Boolean, fileName: String, context: Context): ByteArray? {
    val inputStream = if (fromGallery) getGalleryInputStream(fileName, context) else null
        ?: getFileInputStream(fileName, context)
    return inputStream?.readBytes()?.also { inputStream.close() }
}

fun fileExists(fileName: String, context: Context) = File(getFilePath(context, fileName)).exists()

fun resizeImage(
    uri: Uri? = null,
    path: String? = null,
    context: Context,
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
        val resizedBitmap = Bitmap.createScaledBitmap(fastResizedBitmap!!, width, height, true)
        if (toGallery) {
            saveImageToGallery(resizedBitmap, fileName, context)
        } else {
            saveImageToPath(resizedBitmap, fileName, context)
        }
        return BitmapWithName(fileName, resizedBitmap)
    } catch (e: Exception) {
        Log.w(TAG, "resizeImage", e)
    }
    return null
}


private fun getFileInputStream(fileName: String, context: Context): InputStream? {
    return try {
        FileInputStream(File(getFilePath(context, fileName)))
    } catch (e: FileNotFoundException) {
        Log.w(TAG, "getFileInputStream ${e.message}")
        null
    }
}

private fun saveImageToGallery(bitmap: Bitmap, fileName: String, context: Context) {
    val outputStream = getGalleryOutputStream(fileName, context)
    if (outputStream != null) {
        saveImageToStream(bitmap, outputStream)
    } else {
        saveImageToPath(bitmap, fileName, context)
    }
}

private fun getGalleryInputStream(fileName: String, context: Context): InputStream? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        /*
        val selection =
            "${MediaStore.Images.Media.RELATIVE_PATH} = ? and ${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val imagesPath = "${DIRECTORY_PICTURES}/$GALLERY_FOLDER"
        val selectionArgs = arrayOf(imagesPath, fileName)
         */
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
                    return context.contentResolver.openInputStream(uri)
                } else {
                    Log.w(TAG, "getGalleryInputStream $fileName not found")
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "getGalleryInputStream exception: ${e.message}")
        }
    } else {
        val imagesDir =
            File("${Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)}/$GALLERY_FOLDER")
        return try {
            FileInputStream(File(imagesDir, fileName).path)
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "getGalleryInputStream exception: ${e.message}")
            null
        }
    }
    return null
}

private fun getGalleryOutputStream(fileName: String, context: Context): OutputStream? {
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

private fun saveImageToPath(bitmap: Bitmap, fileName: String, context: Context) {
    val filePath = getFilePath(context, fileName)
    saveImageToStream(bitmap, FileOutputStream(filePath))
}

private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream) {
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    outputStream.flush()
    outputStream.close()
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