package bogomolov.aa.anochat.features.contacts.list.actions

import bogomolov.aa.anochat.features.contacts.list.UsersActionContext
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.isNotValidPhone

class SearchAction(private val query: String) : UserAction<UsersActionContext> {

    override suspend fun execute(context: UsersActionContext) {
        if (isNotValidPhone(query)) {
            val searchedUsers = context.usersList?.filter { it.name.startsWith(query) }
            context.viewModel.setState { copy(searchedUsers = searchedUsers) }
        } else {
            val searchedUsers = context.repository.findByPhone(query)
            for (user in searchedUsers) context.repository.syncFromRemoteUser(user, saveLocal = false, loadFullPhoto = false)
            context.viewModel.setState { copy(searchedUsers = searchedUsers) }
        }
    }
}