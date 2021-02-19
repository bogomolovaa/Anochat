package bogomolov.aa.anochat.features.conversations.dialog

import android.app.Activity
import android.util.DisplayMetrics
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConversationRecyclerViewSetup(
    private val fragment: ConversationFragment,
    private val viewModel: ConversationViewModel,
) {
    private var recyclerViewRestored = false
    private lateinit var binding: FragmentConversationBinding

    fun setup(binding: FragmentConversationBinding, onPreDraw: () -> Unit) {
        this.binding = binding
        recyclerViewRestored = false
        with(binding.recyclerView) {
            setItemViewCacheSize(20)
            adapter = createRecyclerViewAdapter()
            layoutManager = LinearLayoutManager(context)
            addOnScrollListener(createRecyclerViewScrollListener())
            doOnPreDraw { onPreDraw()}
        }
    }

    fun setPagedListLiveData(pagedListLiveData: LiveData<PagedList<MessageView>>) {
        pagedListLiveData.observe(fragment.viewLifecycleOwner) { pagedList ->
            (binding.recyclerView.adapter as MessagesPagedAdapter).submitList(pagedList)
            restoreRecyclerViewPosition()
        }
    }

    fun scrollToEnd() {
        val adapter = binding.recyclerView.adapter!!
        binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    private fun createRecyclerViewAdapter(): MessagesPagedAdapter {
        val data = ActionModeData<MessageView>(R.menu.messages_menu, binding.toolbar)
        data.actionsMap[R.id.delete_messages_action] = { _, items ->
            viewModel.addAction(DeleteMessagesAction(items.map { it.message.id }.toSet()))
        }
        data.actionsMap[R.id.reply_message_action] = { _, items ->
            val message = items.iterator().next()
            onReply(message.message)
        }
        val adapter = MessagesPagedAdapter(
            windowWidth = getWindowWidth(),
            onReply = ::onReply,
            actionModeData = data
        )
        adapter.setHasStableIds(true)
        return adapter
    }

    private fun onReply(message: Message) {
        viewModel.setStateAsync { copy(replyMessage = message) }
        binding.removeReply.setOnClickListener {
            viewModel.setStateAsync { copy(replyMessage = null) }
        }
    }

    private fun getWindowWidth(): Int {
        val displayMetrics = DisplayMetrics()
        val activity = binding.root.context as Activity
        activity.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun createRecyclerViewScrollListener(): RecyclerView.OnScrollListener {
        val linearLayoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
        val adapter = binding.recyclerView.adapter as MessagesPagedAdapter
        var loadImagesJob: Job? = null
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy != 0) fragment.hideKeyBoard()
                val firstId = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                val lastId = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                loadImagesJob?.cancel()
                loadImagesJob = fragment.lifecycleScope.launch {
                    delay(1000)
                    for (id in firstId..lastId) if (id != -1) {
                        val viewHolder = recyclerView.findViewHolderForLayoutPosition(id)
                        if (viewHolder != null) adapter.loadDetailedImage(id, viewHolder)
                    }
                    saveRecyclerViewPosition()
                }
            }
        }
    }

    private fun saveRecyclerViewPosition() {
        viewModel.setStateAsync {
            copy(recyclerViewState = binding.recyclerView.layoutManager?.onSaveInstanceState())
        }
    }

    private fun restoreRecyclerViewPosition() {
        val recyclerViewState = viewModel.state.recyclerViewState
        if (recyclerViewState != null && !recyclerViewRestored) {
            binding.recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
            recyclerViewRestored = true
        } else {
            scrollToEnd()
        }
    }
}