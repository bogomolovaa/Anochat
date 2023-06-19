package bogomolov.aa.anochat.features.contacts.user

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.ImmutableFlow
import bogomolov.aa.anochat.features.shared.asImmutableFlow
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserUiState(
    val user: User? = null,
)

@HiltViewModel
class UserViewViewModel @Inject constructor(
    private val userUseCases: UserUseCases
) : BaseViewModel<UserUiState>(UserUiState()) {

    var pagingFlow: ImmutableFlow<PagingData<String>>? = null

    fun initUser(id: Long) {
        viewModelScope.launch {
            val flow = userUseCases.getImagesDataSource(id).cachedIn(viewModelScope)
            val user = userUseCases.getUser(id)
            pagingFlow = flow.asImmutableFlow()
            setState { copy(user = user) }
        }
    }
}

val testUser by lazy {
    User(
        name = "Alexander1",
        phone = "12334567",
        status = "Status"
    )
}

val testUserUiState by lazy {
    UserUiState(
        user = testUser
    )
}