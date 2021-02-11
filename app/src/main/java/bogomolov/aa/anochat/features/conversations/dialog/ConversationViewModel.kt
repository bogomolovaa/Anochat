package bogomolov.aa.anochat.features.conversations.dialog

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.shared.ActionContext
import bogomolov.aa.anochat.features.shared.BaseViewModel
import bogomolov.aa.anochat.features.shared.UiState
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject


data class DialogUiState(
    val conversation: Conversation? = null,
    val onlineStatus: String = "",
    val pagedListLiveData: LiveData<PagedList<MessageView>>? = null,
    var recyclerViewState: Parcelable? = null
) : UiState

data class ConversationActionContext(
    val viewModel: ConversationViewModel,
    val repository: Repository,
) : ActionContext

class ConversationViewModel
@Inject constructor(private val repository: Repository) :
    BaseViewModel<DialogUiState, ConversationActionContext>() {

    override fun createInitialState() = DialogUiState()
    override fun createViewModelContext() = ConversationActionContext(this, repository)

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(Dispatchers.IO) {
            if (currentState.conversation != null)
                repository.deleteConversationIfNoMessages(currentState.conversation!!.id)
        }
    }
}