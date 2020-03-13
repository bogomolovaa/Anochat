package bogomolov.aa.anochat.android

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlin.reflect.KClass

const val UID = "uid"
const val NOTIFICATIONS = "notifications"
const val SOUND = "sound"
const val VIBRATION = "vibration"

inline fun <reified T> getSetting(context: Context, name: String): T? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (String is T) return sharedPreferences.getString(name, null) as T
    if (Boolean is T) return sharedPreferences.getBoolean(name, false) as T
    return null
}

inline fun <reified T> setSetting(context: Context, name: String, value: T) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (String is T) sharedPreferences.edit { putString(name, value as String) }
    if (Boolean is T) sharedPreferences.edit { putBoolean(name, value as Boolean) }
}