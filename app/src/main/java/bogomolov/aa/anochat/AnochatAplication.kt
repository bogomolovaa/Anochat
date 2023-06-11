package bogomolov.aa.anochat

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.*
import androidx.work.Configuration
import bogomolov.aa.anochat.repository.Firebase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class AnochatAplication: Application(), Configuration.Provider {
    var inBackground = true

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()


    override fun onCreate() {
        super.onCreate()
    }
}