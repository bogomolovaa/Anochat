package bogomolov.aa.anochat.features.contacts.list.actions

import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.contacts.list.UsersActionContext
import bogomolov.aa.anochat.features.shared.UserAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CreateConversationAction(val user: User, val onCreate: (Long) -> Unit) :
    UserAction<UsersActionContext> {

    override suspend fun execute(context: UsersActionContext) {
        val conversationId = context.repository.getConversation(user)
        withContext(Dispatchers.Main) {
            onCreate(conversationId)
        }
    }
}