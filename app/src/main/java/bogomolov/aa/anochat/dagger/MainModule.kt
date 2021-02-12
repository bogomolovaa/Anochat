package bogomolov.aa.anochat.dagger

import android.app.Application
import android.content.Context
import androidx.room.Room
import bogomolov.aa.anochat.repository.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class MainModule {

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

    @Provides
    @Singleton
    fun providesRepository(
        application: Application,
        db: AppDatabase,
        firebase: FirebaseRepository,
        keyValueStore: KeyValueStore,
        crypto: Crypto
    ): Repository =
        RepositoryImpl(db, firebase, keyValueStore, crypto, getFilesDir(application))
}