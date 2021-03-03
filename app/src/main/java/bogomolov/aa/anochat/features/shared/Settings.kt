package bogomolov.aa.anochat.features.shared

import android.content.Context
import bogomolov.aa.anochat.repository.KeyValueStoreImpl


data class Settings(
    val notifications: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val gallery: Boolean = false
) {
    companion object {
        const val NOTIFICATIONS = "notifications"
        const val SOUND = "sound"
        const val VIBRATION = "vibration"
        const val GALLERY = "gallery"

        fun get(name: String, context: Context): Boolean{
            return KeyValueStoreImpl(context).getBooleanValue(name)
        }
    }
}