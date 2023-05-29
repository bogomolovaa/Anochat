package bogomolov.aa.anochat.repositories

import bogomolov.aa.anochat.di.BindsModule
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.shared.LocaleProvider
import bogomolov.aa.anochat.features.shared.LocaleProviderImpl
import bogomolov.aa.anochat.repository.repositories.ConversationRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.MessageRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn


@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BindsModule::class]
)
abstract class FakeBindsModule {

    @Binds
    abstract fun bindsConversationRepository(conversationRepository: ConversationRepositoryImpl): ConversationRepository

    @Binds
    abstract fun bindsUserRepository(userRepository: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindsMessageRepository(messageRepository: MessageRepositoryImpl): MessageRepository

    @Binds
    abstract fun bindsLocaleProvider(localeProvider: LocaleProviderImpl): LocaleProvider
}