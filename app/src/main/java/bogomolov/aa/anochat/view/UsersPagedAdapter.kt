package bogomolov.aa.anochat.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.databinding.UserLayoutBinding

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(
        user1: User,
        user2: User
    ): Boolean =
        user1 == user2

    override fun areContentsTheSame(
        user1: User,
        user2: User
    ): Boolean =
        user1 == user2

}
class UsersPagedAdapter:
    PagedListAdapter<User, UsersPagedAdapter.UserViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.messageCardView
        return UserViewHolder(cv, binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        val cardView: CardView,
        val binding: UserLayoutBinding
    ) : RecyclerView.ViewHolder(cardView) {
        fun bind(user: User?) {
            if (user != null) {
                binding.user = user
            }
        }
    }




}