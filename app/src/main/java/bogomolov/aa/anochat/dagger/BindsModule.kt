package bogomolov.aa.anochat.dagger

import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.FirebaseImpl
import bogomolov.aa.anochat.repository.KeyValueStoreImpl
import bogomolov.aa.anochat.repository.repositories.*
import dagger.Binds
import dagger.Module

@Module
abstract class BindsModule {

    @Binds
    abstract fun bindsConversationRepository(conversationRepository: ConversationRepositoryImpl): ConversationRepository

    @Binds
    abstract fun bindsUserRepository(userRepository: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindsMessageRepository(messageRepository: MessageRepositoryImpl): MessageRepository

    @Binds
    abstract fun bindsAuthRepository(authRepository: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindsKeyValueStore(keyValueStoreImpl: KeyValueStoreImpl): KeyValueStore

    @Binds
    abstract fun bindsFirebase(firebase: FirebaseImpl): Firebase
}