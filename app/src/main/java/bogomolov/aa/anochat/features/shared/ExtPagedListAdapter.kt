package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

abstract class ExtPagedListAdapter<T, B>(
    private val actionModeData: ActionModeData<T>? = null,
    private val onClickListener: ItemClickListener<T>? = null
) : PagedListAdapter<T, ExtPagedListAdapter<T, B>.VH>(createDiffCallback()) {
    private val selectedIds: MutableSet<Long> = HashSet()
    private val selectedItems: MutableSet<T> = HashSet()
    private var selectionMode = false
    private var actionMode: ActionMode? = null

    abstract fun getId(item: T): Long

    abstract fun bind(item: T?, binding: B)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        if (item != null)
            holder.cardView.isChecked = selectedIds.contains(getId(item))
        bind(item, holder.binding)
    }

    inner class VH(
        viewHolder: View,
        val cardView: MaterialCardView,
        val binding: B
    ) : RecyclerView.ViewHolder(viewHolder), View.OnClickListener, View.OnLongClickListener {

        init {
            if (actionModeData != null) cardView.setOnLongClickListener(this)
            if (onClickListener != null) cardView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            val position = adapterPosition
            val item = getItem(position)
            if (item != null) {
                if (selectionMode) {
                    val id = getId(item)
                    if (selectedIds.contains(id)) {
                        selectedIds.remove(id)
                        selectedItems.remove(item)
                    } else {
                        selectedIds.add(id)
                        selectedItems.add(item)
                    }
                    notifyItemChanged(position)
                } else {
                    onClickListener?.onClick(item, view)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            selectionMode = true
            selectedIds.clear()
            selectedItems.clear()
            onClick(view)
            notifyDataSetChanged()
            if (actionModeData != null) {
                if (actionMode == null)
                    actionMode = actionModeData.toolbar.startActionMode(actionModeCallback)
                else
                    actionMode!!.finish()
            }
            return true
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
            for ((actionId, action) in actionModeData?.actionsMap!!) {
                if (item.itemId == actionId) {
                    action(selectedIds, selectedItems)
                    actionMode!!.finish()
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
        selectedIds.clear()
        selectedItems.clear()
        selectionMode = false
        notifyDataSetChanged()
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
    fun onClick(item: T, view: View?)
}

class ActionModeData<T>(val actionModeMenuId: Int, val toolbar: Toolbar) {
    val actionsMap: MutableMap<Int, (Set<Long>, Set<T>) -> Unit> = HashMap()
}