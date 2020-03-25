package bogomolov.aa.anochat.android

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

const val MAX_IMAGE_DIM = 1024

fun isNotValidPhone(string: String) = string.contains("[^+0-9]".toRegex())

fun isValidPhone(string: String) = !isNotValidPhone(string)

fun resizeImage(path: String? = null, context: Context): String {
    var bitmapOptions = BitmapFactory.Options()
    bitmapOptions.inJustDecodeBounds = true
    BitmapFactory.decodeFile(path, bitmapOptions)
    val bitmapWidth = bitmapOptions.outWidth
    val bitmapHeight = bitmapOptions.outHeight

    var ratio = MAX_IMAGE_DIM / max(bitmapWidth, bitmapHeight).toFloat()
    if (ratio > 1) ratio = 1.0f
    val outWidth = (ratio * bitmapWidth).toInt()
    /*
    val resized = Bitmap.createScaledBitmap(
        btmp,
        (ratio * bitmapWidth).toInt(),
        (ratio * bitmapHeight).toInt(),
        true
    )
     */
    bitmapOptions = BitmapFactory.Options()
    bitmapOptions.inScaled = true
    bitmapOptions.inSampleSize = 4
    bitmapOptions.inDensity = bitmapWidth
    bitmapOptions.inTargetDensity =  outWidth * bitmapOptions.inSampleSize
    val resized = BitmapFactory.decodeFile(path, bitmapOptions);
    val fileName = "${getRandomString(20)}.jpg"
    try {
        val stream = FileOutputStream(getFilePath(context, fileName))
        resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.flush()
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return fileName
}

fun getMiniPhotoFileName(context: Context, fileName: String) =
    File(getFilePath(context, fileName)).nameWithoutExtension + "_mini.jpg"

fun getFilePath(context: Context, fileName: String) = File(getFilesDir(context), fileName).path

fun getFilesDir(context: Context) = context.cacheDir

fun getRandomString(length: Int): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz1234567890"
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}


fun getPath(context: Context, uri: Uri): String? {
    // DocumentProvider
    if (DocumentsContract.isDocumentUri(context, uri)) { // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                return context.getExternalFilesDirs(null).toString() + "/" + split[1]
            }
            // TODO handle non-primary volumes
        } else if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri: Uri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
            )
            return getDataColumn(context, contentUri, null, null)
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val selection = "_id=?"
            val selectionArgs: Array<String?> = arrayOf(
                split[1]
            )
            return getDataColumn(context, contentUri, selection, selectionArgs)
        }
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        return getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.getPath()
    }
    return null
}

private fun getDataColumn(
    context: Context, uri: Uri?, selection: String?,
    selectionArgs: Array<String?>?
): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(
        column
    )
    try {
        cursor = context.contentResolver.query(
            uri!!, projection, selection, selectionArgs,
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex: Int = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(columnIndex)
        }
    } finally {
        if (cursor != null) cursor.close()
    }
    return null
}

private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}