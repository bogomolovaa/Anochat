package bogomolov.aa.anochat.features.contacts.list

import android.view.LayoutInflater
import android.view.ViewGroup
import bogomolov.aa.anochat.databinding.UserLayoutBinding
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.ItemClickListener

class UsersAdapter(onClickListener: ItemClickListener<User>) :
    ExtPagedListAdapter<User, UserLayoutBinding>(onClickListener = onClickListener) {
    private val users = ArrayList<User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, binding)
    }

    override fun getElement(position: Int) = users[position]

    override fun getId(item: User) = item.id

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(user: User?, holder: VH) {
        val binding = holder.binding
        if (user != null) {
            binding.userName.text = user.name
            if (user.photo != null) binding.userPhoto.setImage(user.photo)
            binding.userStatus.text = user.status ?: ""
            binding.phoneText.text = user.phone ?: ""
        }
    }

    fun submitList(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = users.size
}