package bogomolov.aa.anochat

import bogomolov.aa.anochat.di.BindsModule
import bogomolov.aa.anochat.di.ProvidesModule
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.shared.AudioPlayer
import bogomolov.aa.anochat.features.shared.AudioPlayerImpl
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.FileStoreImpl
import bogomolov.aa.anochat.repository.KeyValueStoreImpl
import bogomolov.aa.anochat.repository.repositories.AuthRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.ConversationRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.MessageRepositoryImpl
import bogomolov.aa.anochat.repository.repositories.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
    abstract fun bindsKeyValueStore(keyValueStoreImpl: KeyValueStoreImpl): KeyValueStore

    @Binds
    abstract fun bindsFileStore(fileStore: FileStoreImpl): FileStore

    @Binds
    abstract fun bindsAudioPlayer(audioPlayer: AudioPlayerImpl): AudioPlayer
}