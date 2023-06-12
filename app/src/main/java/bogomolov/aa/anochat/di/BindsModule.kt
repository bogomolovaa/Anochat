package bogomolov.aa.anochat.di

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.repository.*
import bogomolov.aa.anochat.repository.repositories.AuthRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.ConversationRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.MessageRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
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
    abstract fun bindsFileStore(fileStore: FileStoreImpl): FileStore

    @Binds
    abstract fun bindsAudioPlayer(audioPlayer: AudioPlayerImpl): AudioPlayer

    @Binds
    abstract fun bindsLocaleProvider(localeProvider: LocaleProviderImpl): LocaleProvider

    @Binds
    abstract fun bindsFirebase(firebase: FirebaseImpl): Firebase
}