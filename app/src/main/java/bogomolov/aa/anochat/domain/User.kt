package bogomolov.aa.anochat.domain

data class User(var id: Long = 0, val uid: String, val phone: String ,val name: String, val photo: String? = null, val status: String? = null){

    //override fun hashCode() : Int = id.toString().hashCode()
}
