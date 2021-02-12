package bogomolov.aa.anochat.features.contacts.list.actions

import androidx.paging.LivePagedListBuilder
import bogomolov.aa.anochat.features.contacts.list.UsersActionContext
import bogomolov.aa.anochat.features.shared.mvi.UserAction

class LoadContactsAction(private val phones: List<String>) : UserAction<UsersActionContext> {

    override suspend fun execute(context: UsersActionContext) {
        val pagedListLiveData =
            LivePagedListBuilder(context.repository.getUsersByPhonesDataSource(phones), 10).build()
        context.viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
        context.usersList = context.repository.updateUsersByPhones(phones)
        context.viewModel.setState { copy(synchronizationFinished = true) }
    }
}