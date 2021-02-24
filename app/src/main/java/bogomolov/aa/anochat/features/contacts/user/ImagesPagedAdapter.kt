package bogomolov.aa.anochat.features.contacts.user

import android.view.LayoutInflater
import android.view.ViewGroup
import bogomolov.aa.anochat.databinding.ImageLayoutBinding
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.ItemClickListener
import bogomolov.aa.anochat.features.shared.getBitmap

class ImagesPagedAdapter(onClickListener: ItemClickListener<String>) :
    ExtPagedListAdapter<String, ImageLayoutBinding>(onClickListener = onClickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ImageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, cv, binding)
    }

    override fun getId(item: String) = 0L

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(image: String?, binding: ImageLayoutBinding) {
        if (image != null) {
            binding.image.transitionName = image
            binding.image.setImageBitmap(getBitmap(image, binding.image.context, 8))
        }
    }
}