package bogomolov.aa.anochat.features.contacts.list.actions

import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.contacts.list.UsersActionContext
import bogomolov.aa.anochat.features.shared.mvi.UserAction


class CreateConversationAction(val user: User) :
    UserAction<UsersActionContext> {

    override suspend fun execute(context: UsersActionContext) {
        val conversationId = context.repository.createConversation(user)
        context.viewModel.setState { copy(conversationId = conversationId) }
    }
}
