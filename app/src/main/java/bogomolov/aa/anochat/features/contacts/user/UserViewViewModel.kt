package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class UserUiState(
    val user: User? = null
) : UiState

class InitUserAction(val id: Long) : UserAction

@HiltViewModel
class UserViewViewModel
@Inject constructor(private val userUseCases: UserUseCases) : BaseViewModel<UserUiState>() {
    private val _imagesLiveData = MediatorLiveData<PagedList<String>>()
    val imagesLiveData: LiveData<PagedList<String>>
        get() = _imagesLiveData

    override fun createInitialState() = UserUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is InitUserAction) action.execute()
    }

    private suspend fun InitUserAction.execute() {
        val liveData =
            LivePagedListBuilder(userUseCases.getImagesDataSource(id), 10).build()
        withContext(Dispatchers.Main) {
            _imagesLiveData.addSource(liveData) { _imagesLiveData.value = it }
        }
        val user = userUseCases.getUser(id)
        setState { copy(user = user) }
    }
}