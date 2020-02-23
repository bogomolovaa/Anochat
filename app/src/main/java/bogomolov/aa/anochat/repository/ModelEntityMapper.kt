package bogomolov.aa.anochat.repository

import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.repository.entity.MessageEntity

fun entityToModel(from: MessageEntity?): Message? =
    if (from != null)
        Message(from.id, from.text, from.time, from.conversationId, from.senderId)
    else null

fun modelToEntity(from: Message): MessageEntity =
        MessageEntity(from.id, from.text, from.time, from.conversationId, from.senderId)

inline fun <reified T> entityToModel(fromList: List<*>): List<T> {
    val toList = ArrayList<T>()
    if (T::class.java.isAssignableFrom(Message::class.java))
        for (fromEntity in fromList) toList.add(entityToModel(fromEntity as MessageEntity) as T)
    return toList
}