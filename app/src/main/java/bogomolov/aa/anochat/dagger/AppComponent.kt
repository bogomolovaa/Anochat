package bogomolov.aa.anochat.dagger

import android.app.Application
import bogomolov.aa.anochat.AnochatAplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidInjectionModule::class, ViewModelsModule::class, InjectionsModule::class, ProvidesModule::class, BindsModule::class])
interface AppComponent : AndroidInjector<AnochatAplication> {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }

    override fun inject(application: AnochatAplication)
}