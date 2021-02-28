package bogomolov.aa.anochat.features.contacts.list

import android.view.LayoutInflater
import android.view.ViewGroup
import bogomolov.aa.anochat.databinding.UserLayoutBinding
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.ItemClickListener

class UsersAdapter(onClickListener: ItemClickListener<User>) :
    ExtPagedListAdapter<User, UserLayoutBinding>(onClickListener = onClickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, binding)
    }

    override fun getId(item: User) = item.id

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(user: User?, holder: VH) {
        holder.binding.user = user
    }
}

class UsersSearchAdapter(onClickListener: ItemClickListener<User>) :
    ExtPagedListAdapter<User, UserLayoutBinding>(onClickListener = onClickListener) {
    private val users = ArrayList<User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, binding)
    }

    override fun getItem(position: Int) = users[position]

    override fun getId(item: User) = item.id

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(user: User?, holder: VH) {
        holder.binding.user = user
    }

    fun submitList(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = users.size
}