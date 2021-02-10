package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.DefaultContext
import bogomolov.aa.anochat.features.shared.RepositoryBaseViewModel
import bogomolov.aa.anochat.features.shared.UiState
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

data class UserUiState(
    val user: User? = null,
    val pagedListLiveData: LiveData<PagedList<String>>? = null
) : UiState

class UserViewViewModel
@Inject constructor(repository: Repository) : RepositoryBaseViewModel<UserUiState>(repository) {
    override fun createInitialState() = UserUiState()
}

class LoadImagesAction(val id: Long) : UserAction<DefaultContext<UserUiState>> {

    override suspend fun execute(context: DefaultContext<UserUiState>) {
        val pagedListLiveData = LivePagedListBuilder(context.repository.getImagesDataSource(id), 10).build()
        context.viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
    }
}

class LoadUserAction(val id: Long) : UserAction<DefaultContext<UserUiState>> {

    override suspend fun execute(context: DefaultContext<UserUiState>) {
        val user = context.repository.getUser(id)
        context.viewModel.setState { copy(user = user) }
    }
}