package bogomolov.aa.anochat.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import bogomolov.aa.anochat.features.contacts.list.UsersViewModel
import bogomolov.aa.anochat.features.contacts.user.UserViewViewModel
import bogomolov.aa.anochat.features.conversations.dialog.ConversationViewModel
import bogomolov.aa.anochat.features.conversations.list.ConversationListViewModel
import bogomolov.aa.anochat.features.conversations.search.MessageSearchViewModel
import bogomolov.aa.anochat.features.login.SignInViewModel
import bogomolov.aa.anochat.features.settings.SettingsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelsModule {

    @Binds
    @IntoMap
    @ViewModelKey(ConversationViewModel::class)
    abstract fun bindConversationViewModel(tagSelectionViewModel: ConversationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConversationListViewModel::class)
    abstract fun bindConversationListViewModel(conversationListViewModel: ConversationListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SignInViewModel::class)
    abstract fun bindSignInViewModel(signInViewModel: SignInViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UsersViewModel::class)
    abstract fun bindUsersViewModel(usersViewModel: UsersViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(settingsViewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserViewViewModel::class)
    abstract fun bindUserViewViewModel(userViewViewModel: UserViewViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MessageSearchViewModel::class)
    abstract fun bindMessageSearchViewModel(messageSearchViewModel: MessageSearchViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}