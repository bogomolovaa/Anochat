package bogomolov.aa.anochat.features.conversations.dialog

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.conversations.dialog.actions.InitConversationAction
import bogomolov.aa.anochat.features.shared.BaseViewModel
import bogomolov.aa.anochat.features.shared.UiState
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


data class DialogUiState(
    val conversation: Conversation? = null,
    val onlineStatus: String = "",
    val pagedListLiveData: LiveData<PagedList<MessageView>>? = null,
    var recyclerViewState: Parcelable? = null
) : UiState


class ConversationViewModel
@Inject constructor(val repository: Repository) :
    BaseViewModel<DialogUiState, ConversationViewModel>() {
    private lateinit var initConversationAction: InitConversationAction

    override fun createInitialState() = DialogUiState()

    override suspend fun handleAction(action: UserAction<ConversationViewModel>) {
        super.handleAction(action)
        if (action is InitConversationAction) initConversationAction = action
    }

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch {
            initConversationAction.onDestroy()
        }
    }
}