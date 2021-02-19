package bogomolov.aa.anochat.features.shared

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

private const val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz1234567890"
private const val MAX_IMAGE_DIM = 1024f

data class BitmapWithName(val name: String, val bitmap: Bitmap)

fun getMiniPhotoFileName(fileName: String) = File(fileName).nameWithoutExtension + "_mini.jpg"

fun getFilePath(context: Context, fileName: String) = File(getFilesDir(context), fileName).path

fun getFilesDir(context: Context): File = context.filesDir

fun getRandomFileName() = (1..20).map { allowedChars.random() }.joinToString(separator = "")

fun getBitmap(fileName: String, context: Context, quality: Int = 1): Bitmap? {
    val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = quality }
    return BitmapFactory.decodeFile(getFilePath(context, fileName), bitmapOptions)
}

fun resizeImage(uri: Uri? = null, path: String? = null, context: Context): BitmapWithName? {
    val dimensionsBitmapOptions = getImageDimensions(uri, path, context)
    val bitmapWidth = dimensionsBitmapOptions.outWidth
    val bitmapHeight = dimensionsBitmapOptions.outHeight

    val ratio = min(MAX_IMAGE_DIM / max(bitmapWidth, bitmapHeight), 1.0f)
    val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = (1 / ratio).toInt() }
    val fastResizedBitmap = loadBitmap(bitmapOptions, uri, path, context)

    val width = (ratio * bitmapWidth).toInt()
    val height = (ratio * bitmapHeight).toInt()
    val fileName = "${getRandomFileName()}.jpg"
    val filePath = getFilePath(context, fileName)
    try {
        val resizedBitmap = Bitmap.createScaledBitmap(fastResizedBitmap!!, width, height, true)
        saveImage(resizedBitmap, filePath)
        return BitmapWithName(fileName, resizedBitmap)
    } catch (e: Exception) {
        Log.w("resizeImage", "${e.message}")
    }
    return null
}

private fun saveImage(bitmap: Bitmap?, filePath: String) {
    val stream = FileOutputStream(filePath)
    bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    stream.flush()
    stream.close()
}

private fun loadBitmap(
    bitmapOptions: BitmapFactory.Options,
    uri: Uri? = null,
    path: String? = null,
    context: Context
): Bitmap? {
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