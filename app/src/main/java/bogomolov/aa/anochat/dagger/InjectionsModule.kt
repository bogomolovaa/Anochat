package bogomolov.aa.anochat.dagger

import bogomolov.aa.anochat.android.MyFirebaseMessagingService
import bogomolov.aa.anochat.view.MainActivity
import bogomolov.aa.anochat.view.fragments.*
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class InjectionsModule {

    @ContributesAndroidInjector
    abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun bindConversationFragment(): ConversationFragment

    @ContributesAndroidInjector
    abstract fun bindConversationsListFragment(): ConversationsListFragment

    @ContributesAndroidInjector
    abstract fun bindUsersFragment(): UsersFragment

    @ContributesAndroidInjector
    abstract fun bindSignInFragment(): SignInFragment

    @ContributesAndroidInjector
    abstract fun bindSignUpFragment(): SignUpFragment

    @ContributesAndroidInjector
    abstract fun bindSendMediaFragment(): SendMediaFragment

    @ContributesAndroidInjector
    abstract fun bindSettingsFragment(): SettingsFragment

    @ContributesAndroidInjector
    abstract fun bindUserViewFragment(): UserViewFragment

    @ContributesAndroidInjector
    abstract fun bindMyFirebaseMessagingService(): MyFirebaseMessagingService

}