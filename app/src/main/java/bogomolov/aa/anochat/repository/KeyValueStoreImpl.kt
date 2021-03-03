package bogomolov.aa.anochat.repository

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.domain.KeyValueStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyValueStoreImpl @Inject constructor(context: Context) : KeyValueStore {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun byteArrayToBase64(array: ByteArray): String =
        Base64.encodeToString(array, Base64.DEFAULT)

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

    override fun setStringValue(key: String, value: String?) {
        preferences.edit(true) { putString(key, value) }
    }

    override fun setBooleanValue(key: String, value: Boolean) {
        preferences.edit(true) { putBoolean(key, value) }
    }
}