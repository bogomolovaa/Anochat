package bogomolov.aa.anochat.features.contacts.user

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.ImageLayoutBinding
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.repository.getFilePath

class ImagesPagedAdapter : ExtPagedListAdapter<String, ImageLayoutBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ImageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.cardView
        return VH(cv, cv, binding)
    }

    override fun getId(item: String) = 0L

    override fun bind(item: String?, binding: ImageLayoutBinding) {
        if (item != null) {
            binding.image.transitionName = item
            setBitmap(binding.image, item)
            setOnClickListener(binding.image, item)
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

    private fun setBitmap(imageView: ImageView, image: String) {
        imageView.setImageBitmap(BitmapFactory.decodeFile(
            getFilePath(imageView.context, image),
            BitmapFactory.Options().apply { inSampleSize = 4 }
        ))
    }
}