package bogomolov.aa.anochat.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import bogomolov.aa.anochat.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProvidesModule {

    @Singleton
    @Provides
    fun providesAppDatabase(application: Application): AppDatabase =
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            DB_NAME
        ).addMigrations(MIGRATION).build() //.fallbackToDestructiveMigration()

    @Provides
    fun providesContext(application: Application): Context = application

    @Singleton
    @Provides
    fun providesFirebase(): Firebase = FirebaseImpl()
}