package bogomolov.aa.anochat.repository

import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.entity.*

fun entityToModel(from: MessageEntity?): Message? =
    if (from != null)
        Message(from.id, from.text, from.time, from.conversationId, from.senderId)
    else null

fun modelToEntity(from: Message) =
    MessageEntity(from.id, from.text, from.time, from.conversationId, from.senderId)

fun entityToModel(from: UserEntity?): User? =
    if (from != null)
        User(from.id, from.uid, from.name, from.changed)
    else null

fun modelToEntity(from: User) = UserEntity(from.id, from.uid, from.name, from.changed)

fun entityToModel(from: ConversationJoined?): Conversation? =
    if (from != null)
        Conversation(
            from.conversation.id,
            entityToModel(from.user)!!,
            entityToModel(from.lastMessage)
        )
    else null


fun modelToEntity(from: Conversation) =
    ConversationEntity(from.id, from.user.id, from.lastMessage?.id ?: 0)


inline fun <reified T> entityToModel(fromList: List<*>): List<T> {
    val toList = ArrayList<T>()
    if (T::class.java.isAssignableFrom(Message::class.java))
        for (fromEntity in fromList) toList.add(entityToModel(fromEntity as MessageEntity) as T)
    if (T::class.java.isAssignableFrom(User::class.java))
        for (fromEntity in fromList) toList.add(entityToModel(fromEntity as UserEntity) as T)
    return toList
}