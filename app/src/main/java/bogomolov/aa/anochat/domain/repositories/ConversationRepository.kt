package bogomolov.aa.anochat.domain.repositories

import androidx.paging.DataSource
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.User

interface ConversationRepository : ConversationUseCasesInRepository{
    fun createOrGetConversation(user: User): Long
}

interface ConversationUseCasesInRepository{
    fun loadConversationsDataSource(): DataSource.Factory<Int, Conversation>

    fun getConversation(id: Long): Conversation
    fun deleteConversations(ids: Set<Long>)
    fun deleteConversationIfNoMessages(conversation: Conversation)
}