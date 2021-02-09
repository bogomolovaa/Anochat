package bogomolov.aa.anochat.features.contacts.list.actions

import bogomolov.aa.anochat.features.contacts.list.UsersActionContext
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.isNotValidPhone

class SearchAction(private val startWith: String) : UserAction<UsersActionContext> {

    override suspend fun execute(context: UsersActionContext) {
        if (isNotValidPhone(startWith)) {
            val searchedUsers = context.usersList?.filter { it.name.startsWith(startWith) }
            context.viewModel.setState { copy(searchedUsers = searchedUsers) }
        } else {
            val searchedUsers = context.repository.findByPhone(startWith)
            for (user in searchedUsers) context.repository.updateUserFrom(user, false)
            context.viewModel.setState { copy(searchedUsers = searchedUsers) }
        }
    }
}