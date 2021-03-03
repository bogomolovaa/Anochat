package bogomolov.aa.anochat.features.contacts.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.ImageLayoutBinding
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery

class ImagesPagedAdapter(private val onClick: () -> Unit) :
    ExtPagedListAdapter<String, ImageLayoutBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ImageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, binding)
    }

    override fun getId(item: String) = 0L

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun bind(image: String?, holder: VH) {
        val binding = holder.binding
        if (image != null) {
            binding.image.transitionName = image
            binding.image.setImageBitmap(getBitmapFromGallery(image, binding.image.context, 8))
            setOnClickListener(binding.image, image)
        }
    }

    private fun setOnClickListener(imageView: ImageView, image: String) {
        imageView.setOnClickListener {
            val navController = imageView.findNavController()
            val extras =
                FragmentNavigator.Extras.Builder().addSharedElement(imageView, image).build()
            onClick()
            navController.navigate(
                R.id.imageViewFragment,
                Bundle().apply {
                    putString("image", image)
                    putInt("quality", 8)
                    putBoolean("gallery", true)
                },
                null,
                extras
            )
        }
    }
}