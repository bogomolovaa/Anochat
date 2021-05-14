package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class UserUiState(
    val user: User? = null,
    val pagingData: PagingData<String>? = null
) : UiState

class InitUserAction(val id: Long) : UserAction

@HiltViewModel
class UserViewViewModel
@Inject constructor(private val userUseCases: UserUseCases) : BaseViewModel<UserUiState>() {

    override fun createInitialState() = UserUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is InitUserAction) action.execute()
    }

    private suspend fun InitUserAction.execute() {
        val pagingData = userUseCases.getImagesDataSource(id).cachedIn(viewModelScope).first()
        val user = userUseCases.getUser(id)
        setState { copy(user = user, pagingData = pagingData) }
    }
}