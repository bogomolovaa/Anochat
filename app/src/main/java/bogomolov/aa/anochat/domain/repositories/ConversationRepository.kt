package bogomolov.aa.anochat.domain.repositories

import androidx.paging.DataSource
import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.User
import kotlinx.coroutines.flow.Flow

interface ConversationRepository : ConversationUseCasesInRepository{
    fun createOrGetConversation(user: User): Long
}

interface ConversationUseCasesInRepository{
    fun loadConversationsDataSource(): Flow<PagingData<Conversation>>

    fun getConversation(id: Long): Conversation?
    fun deleteConversations(ids: Set<Long>)
    fun deleteConversationIfNoMessages(conversationId: Long)
}