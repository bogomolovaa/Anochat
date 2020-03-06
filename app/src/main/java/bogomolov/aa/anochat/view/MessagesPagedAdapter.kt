package bogomolov.aa.anochat.view

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.databinding.MessageLayoutBinding
import com.google.android.material.card.MaterialCardView
import java.io.File

class MessagesPagedAdapter(
    private val activity: Activity,
    private val helper: AdapterHelper<MessageView, MessageLayoutBinding> = AdapterHelper()
) :
    PagedListAdapter<MessageView, AdapterHelper<MessageView, MessageLayoutBinding>.VH>(
        helper.DIFF_CALLBACK
    ), AdapterSelectable<MessageView, MessageLayoutBinding> {

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<MessageView, MessageLayoutBinding>.VH {
        val binding =
            MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.root
        return helper.VH(cv, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<MessageView, MessageLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = super.getItem(position)

    override fun getId(item: MessageView) = item.message.id

    override fun bind(item: MessageView?, binding: MessageLayoutBinding) {
        if (item != null) {
            binding.message = item
            if (item.message.image != null) {
                val file = File(getFilesDir(activity), item.message.image)
                if (file.exists()) {
                    binding.imageView.setImageBitmap(BitmapFactory.decodeFile(file.path))
                    binding.imageView.setOnClickListener {
                        val navController =
                            Navigation.findNavController(activity, R.id.nav_host_fragment)
                        val extras = FragmentNavigator.Extras.Builder()
                            .addSharedElement(binding.imageView, binding.imageView.transitionName)
                            .build()
                        navController.navigate(
                            R.id.imageViewFragment,
                            Bundle().apply { putString("path", file.path) },
                            null,
                            extras)
                    }
                }
            }
        }
    }

}

