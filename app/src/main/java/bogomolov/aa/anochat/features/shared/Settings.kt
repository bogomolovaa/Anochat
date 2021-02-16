package bogomolov.aa.anochat.features.shared

data class Settings(
    val notifications: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true
)