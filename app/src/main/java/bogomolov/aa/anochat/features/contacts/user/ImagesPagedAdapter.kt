package bogomolov.aa.anochat.features.contacts.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.ImageLayoutBinding
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.getBitmap

class ImagesPagedAdapter : ExtPagedListAdapter<String, ImageLayoutBinding>() {

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
            binding.image.setImageBitmap(getBitmap(image, binding.image.context, 4))
            setOnClickListener(binding.image, image)
        }
    }

    private fun setOnClickListener(imageView: ImageView, image: String) {
        imageView.setOnClickListener { view ->
            val extras = FragmentNavigator.Extras.Builder()
                .addSharedElement(imageView, imageView.transitionName)
                .build()
            Navigation.findNavController(view).navigate(
                R.id.imageViewFragment,
                Bundle().apply { putString("image", image) },
                null,
                extras
            )
        }
    }
}