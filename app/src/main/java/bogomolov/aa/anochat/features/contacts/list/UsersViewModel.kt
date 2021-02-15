package bogomolov.aa.anochat.features.contacts.list

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.repository.isNotValidPhone
import bogomolov.aa.anochat.repository.repositories.ConversationRepository
import bogomolov.aa.anochat.repository.repositories.UserRepository
import javax.inject.Inject

data class ContactsUiState(
    val searchedUsers: List<User>? = null,
    val pagedListLiveData: LiveData<PagedList<User>>? = null,
    val conversationId: Long = 0,
    val synchronizationFinished: Boolean = false
) : UiState

class SearchAction(val query: String) : UserAction
class LoadContactsAction(val phones: List<String>) : UserAction
class CreateConversationAction(val user: User) : UserAction

class UsersViewModel
@Inject constructor(
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository
) : BaseViewModel<ContactsUiState>() {
    private var usersList: List<User>? = null

    override fun createInitialState() = ContactsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is SearchAction) action.execute()
        if (action is LoadContactsAction) action.execute()
        if (action is CreateConversationAction) action.execute()
    }

    private suspend fun SearchAction.execute() {
        if (isNotValidPhone(query)) {
            val searchedUsers = usersList?.filter { it.name.startsWith(query) }
            setState { copy(searchedUsers = searchedUsers) }
        } else {
            val searchedUsers = userRepository.searchByPhone(query)
            setState { copy(searchedUsers = searchedUsers) }
        }
    }

    private suspend fun LoadContactsAction.execute() {
        val pagedListLiveData =
            LivePagedListBuilder(userRepository.getUsersByPhonesDataSource(phones), 10).build()
        setState { copy(pagedListLiveData = pagedListLiveData) }
        usersList = userRepository.updateUsersByPhones(phones)
        setState { copy(synchronizationFinished = true) }
    }

    private suspend fun CreateConversationAction.execute() {
        val conversationId = conversationRepository.createOrGetConversation(user)
        setState { copy(conversationId = conversationId) }
    }
}