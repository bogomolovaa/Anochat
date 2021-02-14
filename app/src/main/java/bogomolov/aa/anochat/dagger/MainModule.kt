package bogomolov.aa.anochat.dagger

import android.app.Application
import android.content.Context
import androidx.room.Room
import bogomolov.aa.anochat.repository.*
import bogomolov.aa.anochat.repository.repositories.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class MainModule {

    @Binds
    abstract fun bindsAuthRepository(authRepository: AuthRepositoryImpl): AuthRepository

    @Singleton
    @Provides
    fun providesAppDatabase(application: Application): AppDatabase =
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            DB_NAME
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun providesContext(application: Application): Context = application
}