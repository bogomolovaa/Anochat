package bogomolov.aa.anochat.repository

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import javax.inject.Inject


class KeyValueStore @Inject constructor(val context: Context) {

    inline fun <reified T> getValue(key: String): T? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return when (T::class) {
            ByteArray::class -> {
                val value = sharedPreferences.getString(key, null)
                if (value != null) Crypto.base64ToByteArray(value) as T? else null
            }
            String::class -> sharedPreferences.getString(key, null) as T?
            Boolean::class -> sharedPreferences.getBoolean(key, false) as T
            else -> null
        }
    }

    inline fun <reified T> setValue(key: String, value: T?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        when (T::class) {
            ByteArray::class -> {
                val string = Crypto.byteArrayToBase64(value as ByteArray)
                preferences.edit(true) { putString(key, string) }
            }
            String::class -> preferences.edit(true) { putString(key, value as String?) }
            Boolean::class -> preferences.edit(true) { putBoolean(key, value as Boolean) }
        }
    }
}