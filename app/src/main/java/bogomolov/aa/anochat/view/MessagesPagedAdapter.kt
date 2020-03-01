package bogomolov.aa.anochat.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.databinding.MessageLayoutBinding
import com.google.android.material.card.MaterialCardView

class MessagesPagedAdapter(private val helper: AdapterHelper<Message, MessageLayoutBinding> = AdapterHelper()) :
    PagedListAdapter<Message, AdapterHelper<Message, MessageLayoutBinding>.VH>(
        helper.DIFF_CALLBACK
    ), AdapterSelectable<Message, MessageLayoutBinding> {

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<Message, MessageLayoutBinding>.VH {
        val binding =
            MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.root
        return helper.VH(cv, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<Message, MessageLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = super.getItem(position)

    override fun getId(item: Message) = item.id

    override fun bind(item: Message?, binding: MessageLayoutBinding) {
        if (item != null) {
            binding.message = item
        }
    }

}

