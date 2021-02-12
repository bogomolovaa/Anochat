package bogomolov.aa.anochat.domain

data class Settings(
    val notifications: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true
)