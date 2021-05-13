package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class UserUiState(
    val user: User? = null
) : UiState

class InitUserAction(val id: Long) : UserAction

@HiltViewModel
class UserViewViewModel
@Inject constructor(private val userUseCases: UserUseCases) : BaseViewModel<UserUiState>() {
    private val _imagesLiveData = MediatorLiveData<PagingData<String>>()
    val imagesLiveData: LiveData<PagingData<String>>
        get() = _imagesLiveData

    override fun createInitialState() = UserUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is InitUserAction) action.execute()
    }

    private suspend fun InitUserAction.execute() {
        //todo: paging
        userUseCases.getImagesDataSource(id).cachedIn(viewModelScope).collect {
            _imagesLiveData.postValue(it)
        }
        val user = userUseCases.getUser(id)
        setState { copy(user = user) }
    }
}