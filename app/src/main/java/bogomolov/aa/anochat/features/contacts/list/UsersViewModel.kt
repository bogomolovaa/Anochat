package bogomolov.aa.anochat.features.contacts.list

import android.util.Log
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.entity.isNotValidPhone
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val users: List<User>? = null,
    val conversationId: Long = 0,
    val loading: Boolean = true
) : UiState

class SearchAction(val query: String) : UserAction
class LoadContactsAction(val phones: List<String>) : UserAction
class CreateConversationAction(val user: User) : UserAction
class ResetSearchAction : UserAction

@HiltViewModel
class UsersViewModel
@Inject constructor(
    private val userUseCases: UserUseCases,
    private val conversationUserCases: ConversationUseCases
) : BaseViewModel<ContactsUiState>() {
    private var usersList: List<User>? = null

    override fun createInitialState() = ContactsUiState()

    override suspend fun handleAction(action: UserAction) {
        if (action is SearchAction) action.execute()
        if (action is LoadContactsAction) action.execute()
        if (action is CreateConversationAction) action.execute()
        if (action is ResetSearchAction) action.execute()
    }

    private suspend fun LoadContactsAction.execute() {
        usersList = userUseCases.getUsersByPhones(phones)
        setState { copy(users = usersList) }
        viewModelScope.launch(dispatcher) {
            usersList = userUseCases.updateUsersByPhones(phones)
            setState { copy(loading = false, users = usersList) }
        }
    }

    private suspend fun SearchAction.execute() {
        if (isNotValidPhone(query)) {
            val searchedUsers = usersList?.filter { it.name.startsWith(query) }
            setState { copy(users = searchedUsers) }
        } else {
            setState { copy(loading = true) }
            viewModelScope.launch(dispatcher) {
                val searchedUsers = userUseCases.searchByPhone(query)
                setState { copy(loading = false, users = searchedUsers) }
            }
        }
    }

    private suspend fun ResetSearchAction.execute() {
        setState { copy(users = usersList) }
    }

    private suspend fun CreateConversationAction.execute() {
        setState { copy(loading = true) }
        viewModelScope.launch(dispatcher) {
            val conversationId = conversationUserCases.startConversation(user.uid)
            setState { copy(loading = false, conversationId = conversationId) }
        }
    }
}