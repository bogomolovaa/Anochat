package bogomolov.aa.anochat.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.databinding.UserLayoutBinding

class UsersAdapter :
    RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {
    val users = ArrayList<User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.messageCardView
        return UserViewHolder(cv, binding)
    }

    fun submitList(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    inner class UserViewHolder(
        val cardView: CardView,
        val binding: UserLayoutBinding
    ) : RecyclerView.ViewHolder(cardView) {
        fun bind(user: User?) {
            if (user != null)
                binding.user = user
        }
    }


}