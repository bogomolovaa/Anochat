package bogomolov.aa.anochat.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import bogomolov.aa.anochat.viewmodel.ConversationListViewModel
import bogomolov.aa.anochat.viewmodel.ConversationViewModel
import bogomolov.aa.anochat.viewmodel.MainActivityViewModel
import bogomolov.aa.anochat.viewmodel.SignInViewModel
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
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindMainActivityViewModel(mainActivityViewModel: MainActivityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SignInViewModel::class)
    abstract fun bindSignInViewModel(signInViewModel: SignInViewModel): ViewModel


    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}