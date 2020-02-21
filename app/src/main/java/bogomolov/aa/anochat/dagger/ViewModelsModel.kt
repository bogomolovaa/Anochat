package bogomolov.aa.anochat.dagger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelsModule {
    /*
    @Binds
    @IntoMap
    @ViewModelKey(TagSelectionViewModel::class)
    abstract fun bindTagSelectionViewModel(tagSelectionViewModel: TagSelectionViewModel): ViewModel
    */

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}