package bogomolov.aa.anochat.core

data class Conversation(
    var id: Long,
    var userId: Long,
    var lastMessage: String,
    var lastTime: Long
)