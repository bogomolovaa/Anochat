package bogomolov.aa.anochat.features.shared


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

        fun create() = Settings(
            notifications = false,
            sound = false,
            vibration = false,
            gallery = false
        )
    }
}