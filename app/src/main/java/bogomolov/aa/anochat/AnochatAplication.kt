package bogomolov.aa.anochat

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.features.shared.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AnochatAplication : Application(), Configuration.Provider {
    var inBackground = true

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    internal lateinit var messageUseCases: MessageUseCases

    @Inject
    internal lateinit var authRepository: AuthRepository

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()


    override fun onCreate() {
        super.onCreate()
        authRepository.initAuthListener(messageUseCases)
    }
}