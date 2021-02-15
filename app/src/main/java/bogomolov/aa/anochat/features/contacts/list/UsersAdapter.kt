package bogomolov.aa.anochat.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.databinding.UserLayoutBinding
import bogomolov.aa.anochat.domain.entity.User

class UsersAdapter(private val helper: AdapterHelper<User, UserLayoutBinding> = AdapterHelper()) :
    PagedListAdapter<User, AdapterHelper<User, UserLayoutBinding>.VH>(helper.DIFF_CALLBACK),
    AdapterSelectable<User, UserLayoutBinding> {

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<User, UserLayoutBinding>.VH {
        val binding =
            UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return helper.VH(cv, cv, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<User, UserLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = super.getItem(position)

    override fun getId(item: User) = item.id

    override fun bind(item: User?, binding: UserLayoutBinding) {
        binding.user = item
    }
}

class UsersSearchAdapter(private val helper: AdapterHelper<User, UserLayoutBinding> = AdapterHelper()) :
    RecyclerView.Adapter<AdapterHelper<User, UserLayoutBinding>.VH>(),
    AdapterSelectable<User, UserLayoutBinding> {
    private val users = ArrayList<User>()

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<User, UserLayoutBinding>.VH {
        val binding =
            UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return helper.VH(cv, cv, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<User, UserLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = users[position]

    override fun getId(item: User) = item.id

    override fun bind(item: User?, binding: UserLayoutBinding) {
        binding.user = item
    }

    override fun getItemCount() = users.size

    fun submitList(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

}

