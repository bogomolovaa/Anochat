package bogomolov.aa.anochat.dagger

import bogomolov.aa.anochat.view.MainActivity
import bogomolov.aa.anochat.view.fragments.ConversationFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class InjectionsModule {

    @ContributesAndroidInjector
    abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun bindConversationFragment(): ConversationFragment


}