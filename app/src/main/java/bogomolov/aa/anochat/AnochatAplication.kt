package bogomolov.aa.anochat

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AnochatAplication: Application(), LifecycleObserver, Configuration.Provider {
    var inBackground = true

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()


    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppDestroy() {
        inBackground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppStop() {
        inBackground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAppStart() {
        inBackground = false
    }
}