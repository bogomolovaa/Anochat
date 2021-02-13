package bogomolov.aa.anochat.features.conversations.dialog

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.shared.mvi.RepositoryBaseViewModel
import bogomolov.aa.anochat.features.shared.mvi.UiState
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

class ConversationViewModel @Inject constructor(private val repository: Repository) :
    RepositoryBaseViewModel<DialogUiState>(repository) {

    override fun createInitialState() = DialogUiState()

    override fun onCleared() {
        super.onCleared()
        GlobalScope.launch(Dispatchers.IO) {
            repository.deleteConversationIfNoMessages(currentState.conversation!!)
        }
    }
}