package bogomolov.aa.anochat.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

const val MAX_IMAGE_DIM = 1024

fun getBitmap(fileName: String, quality: Int, context: Context): Bitmap =
    BitmapFactory.decodeFile(getFilePath(context, fileName), BitmapFactory.Options().apply {
        inSampleSize = quality
    })

fun resizeImage(uri: Uri, context: Context): String {
    var bitmapOptions = BitmapFactory.Options()
    bitmapOptions.inJustDecodeBounds = true

    val ins = context.contentResolver.openInputStream(uri)

    BitmapFactory.decodeStream(ins, null, bitmapOptions)
    ins?.close()
    val bitmapWidth = bitmapOptions.outWidth
    val bitmapHeight = bitmapOptions.outHeight

    var ratio = MAX_IMAGE_DIM / max(bitmapWidth, bitmapHeight).toFloat()
    if (ratio > 1) ratio = 1.0f
    val outWidth = (ratio * bitmapWidth).toInt()

    bitmapOptions = BitmapFactory.Options()
    bitmapOptions.inScaled = true
    bitmapOptions.inSampleSize = 4
    bitmapOptions.inDensity = bitmapWidth
    bitmapOptions.inTargetDensity = outWidth * bitmapOptions.inSampleSize

    val ins2 = context.contentResolver.openInputStream(uri)
    val resized = BitmapFactory.decodeStream(ins2, null, bitmapOptions)
    ins2?.close()
    val fileName = "${getRandomString(20)}.jpg"
    try {
        if (resized != null) {
            val stream = FileOutputStream(getFilePath(context, fileName))
            resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.flush()
            stream.close()
        } else {
            throw IOException("can't decode $uri")
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return fileName
}

fun resizeImage(path: String, context: Context): String {
    var bitmapOptions = BitmapFactory.Options()
    bitmapOptions.inJustDecodeBounds = true

    BitmapFactory.decodeFile(path, bitmapOptions)
    val bitmapWidth = bitmapOptions.outWidth
    val bitmapHeight = bitmapOptions.outHeight

    var ratio = MAX_IMAGE_DIM / max(bitmapWidth, bitmapHeight).toFloat()
    if (ratio > 1) ratio = 1.0f
    val outWidth = (ratio * bitmapWidth).toInt()

    bitmapOptions = BitmapFactory.Options()
    bitmapOptions.inScaled = true
    bitmapOptions.inSampleSize = 4
    bitmapOptions.inDensity = bitmapWidth
    bitmapOptions.inTargetDensity = outWidth * bitmapOptions.inSampleSize
    val resized = BitmapFactory.decodeFile(path, bitmapOptions)
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


fun isNotValidPhone(string: String) = string.contains("[^+0-9]".toRegex())

fun isValidPhone(string: String) = !isNotValidPhone(string)


fun getMiniPhotoFileName(fileName: String) =
    File(fileName).nameWithoutExtension + "_mini.jpg"

fun getFilePath(context: Context, fileName: String) = File(getFilesDir(context), fileName).path

fun getFilesDir(context: Context) = context.cacheDir

fun getRandomString(length: Int): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz1234567890"
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}