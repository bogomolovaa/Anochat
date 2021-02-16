package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.setValue
import javax.inject.Inject
import javax.inject.Singleton

const val FILES_DIRECTORY = "files"

@Singleton
class KeyValueStoreImpl @Inject constructor(context: Context) : KeyValueStore {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        val filesDir = getFilesDir(context).path
        setValue(FILES_DIRECTORY, filesDir)
    }

    //java.util.Base64.getEncoder()
    override fun byteArrayToBase64(array: ByteArray): String =
        Base64.encodeToString(array, Base64.DEFAULT)

    //java.util.Base64.getDecoder()
    override fun base64ToByteArray(string: String): ByteArray =
        Base64.decode(string, Base64.DEFAULT)

    override fun getByteArrayValue(key: String) = preferences.getString(key, null)
        ?.let { base64ToByteArray(it) }

    override fun getStringValue(key: String) = preferences.getString(key, null)

    override fun getBooleanValue(key: String) = preferences.getBoolean(key, false)


    override fun setByteArrayValue(key: String, value: ByteArray) {
        val string = byteArrayToBase64(value)
        preferences.edit(true) { putString(key, string) }
    }

    override fun setStringValue(key: String, value: String) {
        //val saved = preferences.edit().putString(key, value).commit()
        //Log.i("test","setStringValue key $key value $value saved $saved")
        preferences.edit(true) { putString(key, value) }
    }

    override fun setBooleanValue(key: String, value: Boolean) {
        preferences.edit(true) { putBoolean(key, value) }
    }
}