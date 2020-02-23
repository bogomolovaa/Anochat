package bogomolov.aa.anochat.core

data class Message(
    var id: Long = 0L,
    var text: String,
    var time: Long,
    var conversationId: Long,
    var senderId: Long = 0L
)
