package bogomolov.aa.anochat.features.contacts.list

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

data class ContactsUiState(
    val searchedUsers: List<User>? = null,
    val pagedListLiveData: LiveData<PagedList<User>>? = null
) : UiState

data class UsersActionContext(
    val viewModel: UsersViewModel,
    val repository: Repository,
    var usersList: List<User>? = null
) : ActionContext

class UsersViewModel
@Inject constructor(private val repository: Repository) :
    BaseViewModel<ContactsUiState, UsersActionContext>() {

    override fun createViewModelContext() = UsersActionContext(this, repository)
    override fun createInitialState() = ContactsUiState()
}