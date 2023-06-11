package bogomolov.aa.anochat.domain.entity

data class User(
    var id: Long = 0,
    val uid: String = "",
    val phone: String? = null,
    val name: String = "",
    val photo: String? = null,
    val status: String? = null
)

fun isNotValidPhone(string: String) = string.contains("[^+0-9]".toRegex())

fun isValidPhone(string: String) = !isNotValidPhone(string) && string.length >= 8