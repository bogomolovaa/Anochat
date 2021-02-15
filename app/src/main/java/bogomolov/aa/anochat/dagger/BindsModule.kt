package bogomolov.aa.anochat.dagger

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.repository.KeyValueStoreImpl
import bogomolov.aa.anochat.repository.repositories.AuthRepository
import bogomolov.aa.anochat.repository.repositories.AuthRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class BindsModule {

    @Binds
    abstract fun bindsAuthRepository(authRepository: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindsKeyValueStore(keyValueStoreImpl: KeyValueStoreImpl): KeyValueStore
}