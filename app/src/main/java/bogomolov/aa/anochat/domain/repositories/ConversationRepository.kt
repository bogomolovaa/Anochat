package bogomolov.aa.anochat.domain.repositories

import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.User
import kotlinx.coroutines.flow.Flow

interface ConversationRepository : ConversationUseCasesInRepository{
    suspend fun createOrGetConversation(user: User): Long
}

interface ConversationUseCasesInRepository{
    fun loadConversationsDataSource(): Flow<PagingData<Conversation>>

    suspend fun getConversation(id: Long): Conversation?
    suspend fun deleteConversations(ids: Set<Long>)
    fun deleteConversationIfNoMessages(conversationId: Long)
}