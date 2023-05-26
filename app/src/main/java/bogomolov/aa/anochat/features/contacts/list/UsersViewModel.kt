package bogomolov.aa.anochat.features.contacts.list

import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.domain.ConversationUseCases
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.entity.isNotValidPhone
import bogomolov.aa.anochat.features.shared.mvi.BaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val users: List<User>? = null,
    val loading: Boolean = true
)

class NavigateConversationEvent(val conversationId: Long) : Event

@HiltViewModel
class UsersViewModel
@Inject constructor(
    private val userUseCases: UserUseCases,
    private val conversationUserCases: ConversationUseCases
) : BaseViewModel<ContactsUiState>(ContactsUiState()) {
    private var usersList: List<User>? = null


    fun loadContacts(phones: List<String>) {
        viewModelScope.launch {
            //TODO: refactor to single method
            usersList = userUseCases.getUsersByPhones(phones)
            setState { copy(users = usersList) }
            usersList = userUseCases.updateUsersByPhones(phones)
            setState { copy(loading = false, users = usersList) }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (isNotValidPhone(query)) {
                val searchedUsers = usersList?.filter { it.name.startsWith(query) }
                setState { copy(users = searchedUsers) }
            } else {
                setState { copy(loading = true) }
                val searchedUsers = userUseCases.searchByPhone(query)
                setState { copy(loading = false, users = searchedUsers) }
            }
        }
    }

    fun resetSearch() {
        setState { copy(users = usersList) }
    }

    fun createConversation(user: User) {
        viewModelScope.launch {
            setState { copy(loading = true) }
            val conversationId = conversationUserCases.startConversation(user.uid)
            addEvent(NavigateConversationEvent(conversationId))
            setState { copy(loading = false) }
        }
    }
}

val testContactsUiState = ContactsUiState(
    users = listOf(
        User(phone = "+12345671", name = "name1", status = "status1"),
        User(phone = "+12345672", name = "name2", status = "status2"),
        User(phone = "+12345673", name = "name3", status = "status3"),
        User(phone = "+12345674", name = "name4", status = "status4"),
        User(phone = "+12345675", name = "name5", status = "status5")
    )
)