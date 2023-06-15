package bogomolov.aa.anochat.domain.entity

data class User(
    val id: Long = 0,
    val uid: String = "",
    val phone: String? = null,
    val name: String = "",
    val photo: String? = null,
    val status: String? = null
)

fun isValidPhone(string: String) = !string.contains("[^+0-9]".toRegex()) && string.length >= 8