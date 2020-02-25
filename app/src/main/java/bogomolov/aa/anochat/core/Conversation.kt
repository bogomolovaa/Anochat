package bogomolov.aa.anochat.core

data class Conversation(
    var id: Long,
    val userId: Long,
    var lastMessage: String,
    var lastTime: Long
)