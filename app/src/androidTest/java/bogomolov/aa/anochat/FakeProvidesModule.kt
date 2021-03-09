package bogomolov.aa.anochat

import android.app.Application
import android.content.Context
import androidx.room.Room
import bogomolov.aa.anochat.di.ProvidesModule
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.DB_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.testing.TestInstallIn

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
}