package bogomolov.aa.anochat.android

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

const val UID = "uid"
const val TOKEN = "token"
const val NOTIFICATIONS = "notifications"
const val SOUND = "sound"
const val VIBRATION = "vibration"

inline fun <reified T> getSetting(context: Context, name: String): T? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (T::class == ByteArray::class) {
        val setting = sharedPreferences.getString(name, null)
        return if (setting != null) base64ToByteArray(setting) as T? else null
    }
    if (T::class == String::class) return sharedPreferences.getString(name, null) as T?
    if (T::class == Boolean::class) return sharedPreferences.getBoolean(name, false) as T
    return null
}

inline fun <reified T> setSetting(context: Context, name: String, value: T?) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (T::class == ByteArray::class) {
        val string = byteArrayToBase64(value as ByteArray)
        sharedPreferences.edit(true) { putString(name, string) }
    }
    if (T::class == String::class) sharedPreferences.edit(true) { putString(name, value as String) }
    if (T::class == Boolean::class) sharedPreferences.edit(true) {
        putBoolean(name, value as Boolean)
    }
}

fun getMyUid(context: Context) = getSetting<String>(context, UID)

fun getSentSettingName(myUid: String, uid: String) = "${myUid}${uid}_sent"


