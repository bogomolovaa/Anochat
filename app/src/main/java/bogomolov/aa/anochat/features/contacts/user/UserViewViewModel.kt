package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.mvi.*
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

data class UserUiState(
    val user: User? = null,
    val pagedListLiveData: LiveData<PagedList<String>>? = null
) : UiState

class LoadImagesAction(val id: Long) : UserAction
class LoadUserAction(val id: Long) : UserAction

class UserViewViewModel
@Inject constructor(private val repository: Repository) : BaseViewModel<UserUiState>() {
    override fun createInitialState() = UserUiState()

    override suspend fun handleAction(action: UserAction) {
        if(action is LoadImagesAction) action.execute()
        if(action is LoadUserAction) action.execute()
    }

    private suspend fun LoadImagesAction.execute() {
        val pagedListLiveData = LivePagedListBuilder(repository.getImagesDataSource(id), 10).build()
        setState { copy(pagedListLiveData = pagedListLiveData) }
    }

    private suspend fun LoadUserAction.execute() {
        val user = repository.getUser(id)
        setState { copy(user = user) }
    }
}