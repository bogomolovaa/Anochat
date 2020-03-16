package bogomolov.aa.anochat.android

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlin.reflect.KClass

const val UID = "uid"
const val TOKEN = "token"
const val NOTIFICATIONS = "notifications"
const val SOUND = "sound"
const val VIBRATION = "vibration"

inline fun <reified T> getSetting(context: Context, name: String): T? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (T::class == String::class) return sharedPreferences.getString(name, null) as T
    if (T::class == Boolean::class) return sharedPreferences.getBoolean(name, false) as T
    return null
}

inline fun <reified T> setSetting(context: Context, name: String, value: T) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (T::class == String::class) sharedPreferences.edit(true) { putString(name, value as String) }
    if (T::class == Boolean::class) sharedPreferences.edit(true) { putBoolean(name, value as Boolean) }
}