package bogomolov.aa.anochat.dagger

import bogomolov.aa.anochat.features.contacts.user.UserViewFragment
import bogomolov.aa.anochat.features.contacts.list.UsersFragment
import bogomolov.aa.anochat.features.conversations.MyFirebaseMessagingService
import bogomolov.aa.anochat.features.conversations.dialog.ConversationFragment
import bogomolov.aa.anochat.features.conversations.search.MessageSearchFragment
import bogomolov.aa.anochat.features.conversations.dialog.media.SendMediaFragment
import bogomolov.aa.anochat.features.conversations.list.ConversationListFragment
import bogomolov.aa.anochat.features.login.SignInFragment
import bogomolov.aa.anochat.features.settings.SettingsFragment
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.features.settings.MiniatureFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class InjectionsModule {

    @ContributesAndroidInjector
    abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun bindConversationFragment(): ConversationFragment

    @ContributesAndroidInjector
    abstract fun bindConversationsListFragment(): ConversationListFragment

    @ContributesAndroidInjector
    abstract fun bindUsersFragment(): UsersFragment

    @ContributesAndroidInjector
    abstract fun bindSignInFragment(): SignInFragment

    @ContributesAndroidInjector
    abstract fun bindSendMediaFragment(): SendMediaFragment

    @ContributesAndroidInjector
    abstract fun bindSettingsFragment(): SettingsFragment

    @ContributesAndroidInjector
    abstract fun bindUserViewFragment(): UserViewFragment

    @ContributesAndroidInjector
    abstract fun bindMessageSearchFragment(): MessageSearchFragment

    @ContributesAndroidInjector
    abstract fun bindMiniatureFragment(): MiniatureFragment

    @ContributesAndroidInjector
    abstract fun bindMyFirebaseMessagingService(): MyFirebaseMessagingService

}