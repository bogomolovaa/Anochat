package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Log
import bogomolov.aa.anochat.features.shared.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FileStoreImpl"

interface FileStore {
    fun saveByteArray(byteArray: ByteArray, fileName: String, toGallery: Boolean)
    fun getByteArray(fromGallery: Boolean, fileName: String): ByteArray?
    fun fileExists(fileName: String): Boolean
}

@Singleton
class FileStoreImpl @Inject constructor(@ApplicationContext private val context: Context) :
    FileStore {

    override fun saveByteArray(byteArray: ByteArray, fileName: String, toGallery: Boolean) {
        if (toGallery && Settings.get(Settings.GALLERY, context))
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
}