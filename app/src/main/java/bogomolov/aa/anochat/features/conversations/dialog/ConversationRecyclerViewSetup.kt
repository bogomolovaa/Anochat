package bogomolov.aa.anochat.features.conversations.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Parcelable
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.ActionModeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConversationRecyclerViewSetup(
    private val fragment: ConversationFragment,
    private val viewModel: ConversationViewModel,
) {
    private var recyclerViewRestored = false
    private var enterAnimationFinished = false
    private lateinit var binding: FragmentConversationBinding
    private lateinit var messagesPagedAdapter: MessagesPagedAdapter

    fun setup(binding: FragmentConversationBinding, onPreDraw: () -> Unit) {
        this.binding = binding
        recyclerViewRestored = false
        enterAnimationFinished = false
        messagesPagedAdapter = createRecyclerViewAdapter()
        with(binding.recyclerView) {
            setItemViewCacheSize(0)
            adapter = messagesPagedAdapter
            layoutManager = object : LinearLayoutManager(context, RecyclerView.VERTICAL, true) {
                override fun isAutoMeasureEnabled() = true
            }
            addOnScrollListener(createRecyclerViewScrollListener())
            doOnPreDraw {
                onPreDraw()
                val animationDuration = resources.getInteger(R.integer.animation_duration).toLong()
                fragment.lifecycleScope.launch {
                    delay(animationDuration)
                    requestLayout()
                }
            }
        }
        observeMessagesLiveData(viewModel.messagesLiveData)
    }

    fun getMessageAudioView(messageId: String) = messagesPagedAdapter.messagesMap[messageId]

    fun getReplyMessageAudioView(messageId: String) =
        messagesPagedAdapter.replyMessagesMap[messageId]

    fun scrollToEnd() {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            delay(100)
            binding.recyclerView.scrollToPosition(0)
        }
    }

    private fun observeMessagesLiveData(pagedListLiveData: LiveData<PagedList<MessageView>>) {
        pagedListLiveData.observe(fragment.viewLifecycleOwner) { pagedList ->
            (binding.recyclerView.adapter as MessagesPagedAdapter).submitList(pagedList)
            restoreRecyclerViewPosition()
        }
    }

    private fun createRecyclerViewAdapter(): MessagesPagedAdapter {
        val data = ActionModeData<MessageView>(R.menu.messages_menu, binding.toolbar)
        data.actionsMap[R.id.delete_messages_action] = { _, items ->
            viewModel.addAction(DeleteMessagesAction(items.map { it.message.id }.toSet()))
        }
        data.actionsMap[R.id.reply_message_action] = { _, items ->
            if (items.isNotEmpty()) {
                val message = items.last()
                onReply(message.message)
            }
        }
        data.actionsMap[R.id.copy_messages_action] = { _, items ->
            val context = fragment.requireContext()
            val text = items.first().message.text
            val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", text)
            clipBoard.setPrimaryClip(clipData)
            val messageText = context.resources.getString(R.string.copied)
            Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
        }
        return MessagesPagedAdapter(
            lifecycleScope = fragment.lifecycleScope,
                    windowWidth = getWindowWidth(),
            onReply = ::onReply,
            actionExecutor = viewModel,
            actionModeData = data
        )
    }

    private fun onReply(message: Message) {
        viewModel.setStateAsync { copy(replyMessage = message) }
        binding.removeReply.setOnClickListener {
            viewModel.setStateAsync { copy(replyMessage = null) }
        }
    }

    private fun getWindowWidth(): Int {
        val displayMetrics = DisplayMetrics()
        fragment.requireActivity().windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun createRecyclerViewScrollListener(): RecyclerView.OnScrollListener {
        val linearLayoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
        val adapter = binding.recyclerView.adapter as MessagesPagedAdapter
        var loadImagesJob: Job? = null
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                //Log.i("test", "onScrolled [$dx,$dy]")
                if (dy != 0) fragment.hideKeyBoard()
                val firstId = linearLayoutManager.findFirstCompletelyVisibleItemPosition() - 1
                val lastId = linearLayoutManager.findLastCompletelyVisibleItemPosition() + 1
                loadImagesJob?.cancel()
                loadImagesJob = fragment.lifecycleScope.launch {
                    if (enterAnimationFinished) delay(300)
                    val saveState = binding.recyclerView.layoutManager?.onSaveInstanceState()
                    for (id in firstId..lastId) if (id != -1) {
                        val viewHolder = recyclerView.findViewHolderForLayoutPosition(id)
                        if (viewHolder != null) adapter.loadDetailed(id, viewHolder)
                    }
                    enterAnimationFinished = true
                    if (dy != 0) saveRecyclerViewPosition(saveState)
                }
            }
        }
    }

    private fun saveRecyclerViewPosition(state: Parcelable?) {
        viewModel.setStateAsync { copy(recyclerViewState = state) }
    }

    private fun restoreRecyclerViewPosition() {
        val recyclerViewState = viewModel.state.recyclerViewState
        if (recyclerViewState != null && !recyclerViewRestored) {
            binding.recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
            recyclerViewRestored = true
        } else {
            binding.messageInputText.requestFocus()
            scrollToEnd()
        }
    }
}