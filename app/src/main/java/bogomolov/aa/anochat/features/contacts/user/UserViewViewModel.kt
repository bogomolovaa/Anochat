package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class UserUiState(
    val user: User? = null,
    val pagedListLiveData: LiveData<PagedList<String>>? = null
) : UiState

class LoadImagesAction(val id: Long) : UserAction
class LoadUserAction(val id: Long) : UserAction

@HiltViewModel
class UserViewViewModel
@Inject constructor(private val userUseCases: UserUseCases) : BaseViewModel<UserUiState>() {
    override fun createInitialState() = UserUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is LoadImagesAction) action.execute()
        if (action is LoadUserAction) action.execute()
    }

    private suspend fun LoadImagesAction.execute() {
        val pagedListLiveData =
            LivePagedListBuilder(userUseCases.getImagesDataSource(id), 10).build()
        setState { copy(pagedListLiveData = pagedListLiveData) }
    }

    private suspend fun LoadUserAction.execute() {
        val user = userUseCases.getUser(id)
        setState { copy(user = user) }
    }
}