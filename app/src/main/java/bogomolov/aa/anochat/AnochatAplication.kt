package bogomolov.aa.anochat

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import bogomolov.aa.anochat.dagger.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class AnochatAplication: Application(), HasAndroidInjector, LifecycleObserver {
    var inBackground = false

    @Inject
    internal lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        DaggerAppComponent.builder().application(this).build().inject(this)
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        inBackground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        inBackground = false
    }
}