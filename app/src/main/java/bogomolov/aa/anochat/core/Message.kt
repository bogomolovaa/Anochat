package bogomolov.aa.anochat.core

data class Message(
    var id: Long = 0L,
    val text: String,
    val time: Long,
    val conversationId: Long,
    val senderId: Long = 0L
)
