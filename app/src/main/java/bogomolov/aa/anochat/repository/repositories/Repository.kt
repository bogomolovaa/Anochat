package bogomolov.aa.anochat.repository.repositories

import javax.inject.Inject
import javax.inject.Singleton

interface Repository {
    val userRepository: UserRepository
    val messageRepository: MessageRepository
    val conversationRepository: ConversationRepository
    val authRepository: AuthRepository
}

@Singleton
class RepositoryImpl @Inject constructor(
    override val userRepository: UserRepository,
    override val conversationRepository: ConversationRepository,
    override val messageRepository: MessageRepository,
    override val authRepository: AuthRepository
) : Repository