package bogomolov.aa.anochat.view

import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

interface AdapterSelectable<T> {
    fun getItem(position: Int): T
    fun notifyDataSetChanged()
    fun notifyItemChanged(position: Int)
}

interface AdapterWithViewHolder<T> {
    fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerAdapterHelper<T>.HelperViewHolder

    fun onBindViewHolder(holder: RecyclerAdapterHelper<T>.HelperViewHolder, position: Int)
}

class RecyclerAdapterHelper<T>(
    val menuId: Int,
    val actionsMap: Map<Int, (Set<Long>) -> Unit>,
    val adapter: AdapterSelectable<T>,
    val toolbar: Toolbar,
    val getId: (T) -> Long,
    val bind: (T?, ViewDataBinding) -> Unit,
    val getDataBinding: (ViewGroup) -> ViewDataBinding,
    val getCardView: (ViewDataBinding) -> MaterialCardView
) : AdapterWithViewHolder<T> {
    private val selectedIds: MutableSet<Long> = HashSet()
    private var selectionMode = false
    private var actionMode: ActionMode? = null


    private val callback = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(menuId, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            for ((actionId, action) in actionsMap) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelperViewHolder {
        val binding = getDataBinding(parent)
        return HelperViewHolder(getCardView(binding), binding)
    }

    override fun onBindViewHolder(holder: HelperViewHolder, position: Int) {
        val item = adapter.getItem(position)
        val cardView = holder.cardView
        if (item != null) {
            val selected = selectedIds.contains(getId(item))
            cardView.isChecked = selected
        }
        holder.bindItem(item)
    }

    inner class HelperViewHolder(val cardView: MaterialCardView, val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(cardView),
        View.OnClickListener, View.OnLongClickListener {

        init {
            cardView.setOnClickListener(this)
            cardView.setOnLongClickListener(this)
        }

        fun bindItem(item: T?) {
            bind(item, binding)
        }

        override fun onClick(v: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            val position = adapterPosition
            val item = adapter.getItem(position)
            if (item != null) {
                if (selectionMode) {
                    if (selectedIds.contains(getId(item))) {
                        selectedIds.remove(getId(item))
                    } else {
                        selectedIds.add(getId(item))
                    }
                    adapter.notifyItemChanged(position)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            selectionMode = true
            selectedIds.clear()
            onClick(view)
            adapter.notifyDataSetChanged()
            if (actionMode == null)
                actionMode = toolbar.startActionMode(callback)
            else
                actionMode!!.finish()
            return true
        }
    }


}
