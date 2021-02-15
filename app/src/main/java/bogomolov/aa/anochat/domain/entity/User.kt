package bogomolov.aa.anochat.domain.entity

data class User(
    var id: Long = 0,
    val uid: String = "",
    val phone: String? = null,
    val name: String = "",
    val photo: String? = null,
    val status: String? = null
) {

    //override fun hashCode() : Int = id.toString().hashCode()
}
