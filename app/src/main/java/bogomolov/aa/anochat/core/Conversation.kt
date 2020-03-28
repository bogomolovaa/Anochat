package bogomolov.aa.anochat.core

data class Conversation(
    var id: Long = 0,
    val user: User,
    var lastMessage: Message? = null
){

    override fun hashCode() : Int = id.toString().hashCode()
}