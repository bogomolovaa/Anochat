package bogomolov.aa.anochat.domain

data class User(var id: Long = 0, val uid: String, val phone: String ,var name: String, var photo: String? = null, var status: String? = null){

    //override fun hashCode() : Int = id.toString().hashCode()
}
