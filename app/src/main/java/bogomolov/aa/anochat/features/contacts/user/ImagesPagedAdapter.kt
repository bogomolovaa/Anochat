package bogomolov.aa.anochat.features.contacts.user

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.paging.PagedListAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.ImageLayoutBinding
import bogomolov.aa.anochat.repository.getFilePath
import bogomolov.aa.anochat.view.adapters.AdapterHelper
import bogomolov.aa.anochat.view.adapters.AdapterSelectable

class ImagesPagedAdapter(
    val context: Context,
    private val helper: AdapterHelper<String, ImageLayoutBinding> = AdapterHelper()
) :
    PagedListAdapter<String, AdapterHelper<String, ImageLayoutBinding>.VH>(
        helper.DIFF_CALLBACK
    ),
    AdapterSelectable<String, ImageLayoutBinding> {

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<String, ImageLayoutBinding>.VH {
        val binding =
            ImageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return helper.VH(cv, cv, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<String, ImageLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = super.getItem(position)

    override fun getId(item: String) = 0L

    override fun bind(item: String?, binding: ImageLayoutBinding) {
        if (item != null) {
            binding.image.transitionName = item
            binding.image.setImageBitmap(BitmapFactory.decodeFile(
                getFilePath(context, item),
                BitmapFactory.Options().apply { inSampleSize = 4 }
            ))
            binding.image.setOnClickListener { view ->
                val extras = FragmentNavigator.Extras.Builder()
                    .addSharedElement(binding.image, binding.image.transitionName)
                    .build()
                Navigation.findNavController(view).navigate(
                    R.id.imageViewFragment,
                    Bundle().apply { putString("image", item) },
                    null,
                    extras
                )
            }
        }
    }

}