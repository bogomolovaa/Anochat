package bogomolov.aa.anochat.repository

import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.repository.entity.ConversationJoined
import bogomolov.aa.anochat.repository.entity.MessageEntity
import bogomolov.aa.anochat.repository.entity.MessageJoined
import bogomolov.aa.anochat.repository.entity.UserEntity

open class ModelEntityMapper {

    fun entityToModel(from: MessageEntity?): Message? =
        if (from != null)
            Message(
                id = from.id,
                text = from.text,
                time = from.time,
                conversationId = from.conversationId,
                isMine = from.isMine == 1,
                messageId = from.messageId,
                image = from.image,
                audio = from.audio,
                publicKey = from.publicKey,
                sent = from.sent,
                received = from.received,
                viewed = from.viewed
            )
        else null

    fun entityToModel(from: MessageJoined?): Message? =
        if (from != null)
            Message(
                from.id,
                from.text,
                from.time,
                from.conversationId,
                from.isMine == 1,
                from.messageId,
                entityToModel(from.replyMessage),
                from.replyMessage?.messageId,
                from.image,
                from.audio,
                from.publicKey,
                from.sent,
                from.received,
                from.viewed
            )
        else null

    fun modelToEntity(from: Message) =
        MessageEntity(
            from.id,
            from.text,
            from.time,
            from.conversationId,
            if (from.isMine) 1 else 0,
            from.messageId,
            from.replyMessage?.messageId ?: from.replyMessageId,
            from.image,
            from.audio,
            from.publicKey,
            from.sent,
            from.received,
            from.viewed
        )

    fun entityToModel(from: UserEntity?): User? =
        if (from != null)
            User(from.id, from.uid, from.phone, from.name, from.photo, from.status)
        else null

    fun modelToEntity(from: User) =
        UserEntity(from.id, from.uid, from.phone, from.name, from.photo, from.status)

    fun entityToModel(from: ConversationJoined?): Conversation? =
        if (from != null)
            Conversation(
                from.conversation.id,
                entityToModel(from.user)!!,
                entityToModel(from.lastMessage)
            )
        else null


    inline fun <reified T> entityToModel(fromList: List<*>): List<T> {
        val toList = ArrayList<T>()
        if (T::class.java.isAssignableFrom(Message::class.java))
            for (fromEntity in fromList) toList.add(entityToModel(fromEntity as MessageEntity) as T)
        if (T::class.java.isAssignableFrom(User::class.java))
            for (fromEntity in fromList) toList.add(entityToModel(fromEntity as UserEntity) as T)
        if (T::class.java.isAssignableFrom(Conversation::class.java))
            for (fromEntity in fromList) toList.add(entityToModel(fromEntity as ConversationJoined) as T)
        return toList
    }

}