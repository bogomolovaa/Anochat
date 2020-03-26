package bogomolov.aa.anochat.view.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

interface AdapterSelectable<T, R> {
    fun getItem(position: Int): T?
    fun notifyDataSetChanged()
    fun notifyItemChanged(position: Int)
    fun bind(item: T?, binding: R)
    fun getId(item: T): Long
}

class AdapterHelper<T, R> constructor(
    val menuId: Int? = null,
    val actionsMap: Map<Int, (Set<Long>) -> Unit>? = null,
    val toolbar: Toolbar? = null,
    val onClick: ((T) -> Unit)? = null
) {
    private val selectedIds: MutableSet<Long> = HashSet()
    private var selectionMode = false
    private var actionMode: ActionMode? = null
    lateinit var adapter: AdapterSelectable<T, R>

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(message1: T, message2: T): Boolean =
            message1 == message2

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(message1: T, message2: T): Boolean =
            message1 == message2

    }

    private val callback = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(menuId!!, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Log.i("test","onActionItemClicked")
            for ((actionId, action) in actionsMap!!) {
                if (item.itemId == actionId) {
                    action(selectedIds)
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
        selectionMode = false
        adapter.notifyDataSetChanged()
    }

    fun onBindViewHolder(holder: VH, position: Int) {
        val item = adapter.getItem(position)
        if (item != null) {
            val selected = selectedIds.contains(adapter.getId(item))
            Log.i("test","CHECKED")
            holder.cardView.isChecked = selected
        }
        holder.bindItem(item)
    }

    inner class VH(viewHolder: View, val cardView: MaterialCardView, val binding: R) :
        RecyclerView.ViewHolder(viewHolder),
        View.OnClickListener, View.OnLongClickListener {

        init {
            if(onClick!=null) cardView.setOnClickListener(this)
            if(actionsMap!=null) cardView.setOnLongClickListener(this)
        }

        fun bindItem(item: T?) {
            adapter.bind(item, binding)
        }

        override fun onClick(v: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            val position = adapterPosition
            val item = adapter.getItem(position)
            if (item != null) {
                if (selectionMode) {
                    if (selectedIds.contains(adapter.getId(item))) {
                        selectedIds.remove(adapter.getId(item))
                    } else {
                        selectedIds.add(adapter.getId(item))
                    }
                    adapter.notifyItemChanged(position)
                }else{
                    onClick?.invoke(item)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            Log.i("test","onLongClick")
            selectionMode = true
            selectedIds.clear()
            onClick(view)
            adapter.notifyDataSetChanged()
            if (actionMode == null)
                actionMode = toolbar?.startActionMode(callback)
            else
                actionMode!!.finish()
            return true
        }
    }


}
