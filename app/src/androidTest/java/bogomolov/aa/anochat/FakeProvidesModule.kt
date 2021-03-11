package bogomolov.aa.anochat

import android.app.Application
import android.content.Context
import androidx.room.Room
import bogomolov.aa.anochat.di.ProvidesModule
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.features.shared.AudioPlayer
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ProvidesModule::class]
)
object FakeProvidesModule {

    @Singleton
    @Provides
    fun providesAppDatabase(application: Application): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            application,
            AppDatabase::class.java,
        ).build()

    @Provides
    fun providesContext(application: Application): Context = application

    @Singleton
    @Provides
    fun providesFirebase(): Firebase = Mockito.mock(Firebase::class.java)

    @Singleton
    @Provides
    fun providesAuthRepository(): AuthRepository = Mockito.mock(AuthRepository::class.java)

    @Singleton
    @Provides
    fun providesKeyValueStore(): KeyValueStore = MockKeyValueStore()

    @Singleton
    @Provides
    fun providesAudioPlayer(): AudioPlayer = Mockito.mock(AudioPlayer::class.java)

    @Singleton
    @Provides
    fun providesFileStore(): FileStore = Mockito.mock(FileStore::class.java)
}