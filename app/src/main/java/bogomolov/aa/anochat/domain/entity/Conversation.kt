package bogomolov.aa.anochat.domain.entity

data class Conversation(
    val id: Long = 0,
    val user: User,
    var lastMessage: Message? = null
)