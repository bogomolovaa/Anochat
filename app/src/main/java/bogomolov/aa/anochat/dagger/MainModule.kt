package bogomolov.aa.anochat.dagger

import android.app.Application
import androidx.room.Room
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.DB_NAME
import bogomolov.aa.anochat.repository.Repository
import bogomolov.aa.anochat.repository.RepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides


@Module
abstract class MainModule {
    @Binds
    abstract fun bindsRepository(repository: RepositoryImpl): Repository

    @Module
    companion object {
        @JvmStatic
        @Provides
        fun providesAppDatabase(application: Application): AppDatabase =
            Room.databaseBuilder(application, AppDatabase::class.java, DB_NAME).build()
    }
}