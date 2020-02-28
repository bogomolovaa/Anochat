package bogomolov.aa.anochat.core

data class Conversation(
    var id: Long,
    val user: User,
    var lastMessage: Message
)