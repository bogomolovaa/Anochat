package bogomolov.aa.anochat.domain.entity

data class Settings(
    val notifications: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true
)