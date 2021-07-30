package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class UserUiState(
    val user: User? = null,
    val pagingFlow: Flow<PagingData<String>>? = null
)

@HiltViewModel
class UserViewViewModel @Inject constructor(
    private val userUseCases: UserUseCases
) : BaseViewModel<UserUiState>(UserUiState()) {

    fun initUser(id: Long) = execute {
        val flow = userUseCases.getImagesDataSource(id).cachedIn(viewModelScope)
        val user = userUseCases.getUser(id)
        setState { copy(user = user, pagingFlow = flow) }
    }
}

val testUserUiState = UserUiState(
    user = User(
        name = "Alexander1",
        phone = "12334567",
        status = "Status"
    )
)