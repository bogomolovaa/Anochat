package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.paging.PagedListAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class ExtPagedListAdapter<T: Any, B>(
    private val actionModeData: ActionModeData<T>? = null,
    private val onClickListener: ItemClickListener<T>? = null
) : PagingDataAdapter<T, ExtPagedListAdapter<T, B>.VH>(createDiffCallback()) {
    private val selectedIds: MutableSet<Long> = HashSet()
    private val selectedItems: MutableSet<T> = HashSet()
    private val selectedVH: MutableSet<VH> = HashSet()
    var selectionMode = false
    private var actionMode: ActionMode? = null

    abstract fun getId(item: T): Long

    abstract fun bind(item: T?, holder: VH)

    protected fun isChecked(item: T) =
        if (item != null) selectedIds.contains(getId(item)) else false

    protected open fun getElement(position: Int) = getItem(position)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getElement(position)
        bind(item, holder)
        onItemSelected(holder.binding, if (item != null) isChecked(item) else false)
    }

    protected open fun onItemSelected(binding: B, selected: Boolean) {}

    @SuppressLint("ClickableViewAccessibility")
    inner class VH(
        viewHolder: View,
        val binding: B
    ) : RecyclerView.ViewHolder(viewHolder) {

        init {
            if (actionModeData != null || onClickListener != null)
                viewHolder.setOnClickListener { onClick() }

            if (actionModeData != null)
                viewHolder.setOnLongClickListener {
                    onLongClick()
                    true
                }
        }

        fun onClick() {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            val item = getElement(adapterPosition)
            if (item != null) {
                if (selectionMode) {
                    val id = getId(item)
                    if (selectedIds.contains(id)) {
                        selectedIds.remove(id)
                        selectedItems.remove(item)
                        selectedVH.remove(this)
                    } else {
                        selectedIds.add(id)
                        selectedItems.add(item)
                        selectedVH.add(this)
                    }
                    onItemSelected(binding, isChecked(item))
                } else {
                    onClickListener?.onClick(item)
                }
            }
        }

        fun onLongClick() {
            selectionMode = true
            selectedIds.clear()
            selectedItems.clear()
            onClick()
            if (actionModeData != null) {
                if (actionMode == null)
                    actionMode = actionModeData.toolbar.startActionMode(actionModeCallback)
                else
                    actionMode?.finish()
            }
        }
    }


    private val actionModeCallback = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(actionModeData?.actionModeMenuId!!, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (actionModeData != null)
                for ((actionId, action) in actionModeData.actionsMap) {
                    if (item.itemId == actionId) {
                        action(HashSet(selectedIds), HashSet(selectedItems))
                        actionMode?.finish()
                        break
                    }
                }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            disableCheckMode()
            actionMode = null
        }
    }

    private fun disableCheckMode() {
        for (vh in selectedVH) onItemSelected(vh.binding, false)
        selectedIds.clear()
        selectedItems.clear()
        selectedVH.clear()
        selectionMode = false
    }
}

private fun <T> createDiffCallback() = object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(item1: T, item2: T): Boolean =
        item1 == item2

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(item1: T, item2: T): Boolean =
        item1 == item2
}

fun interface ItemClickListener<T> {
    fun onClick(item: T)
}

class ActionModeData<T>(val actionModeMenuId: Int, val toolbar: Toolbar) {
    val actionsMap: MutableMap<Int, (Set<Long>, Set<T>) -> Unit> = HashMap()
}