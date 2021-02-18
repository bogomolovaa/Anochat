package bogomolov.aa.anochat.features.conversations.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.core.view.GestureDetectorCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.MessageLayoutBinding
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.ActionModeData
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.ItemClickListener
import bogomolov.aa.anochat.repository.getFilesDir
import com.google.android.material.card.MaterialCardView
import java.io.File

class MessagesPagedAdapter(
    private val windowWidth: Int,
    private val onReply: (Message) -> Unit,
    actionModeData: ActionModeData<MessageView>? = null,
) : ExtPagedListAdapter<MessageView, MessageLayoutBinding>(actionModeData) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cv = binding.messageCardView
        return VH(binding.root, cv, binding)
    }

    override fun getId(item: MessageView) = item.message.id

    override fun getItemId(position: Int) = getItem(position)?.message?.id ?: 0

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(item: MessageView?, binding: MessageLayoutBinding) {
        if (item != null) {
            //Char.isSurrogate()
            binding.message = item
            binding.executePendingBindings()
            val image = item.message.image
            if (image != null) {
                loadImage(image, binding.imageView, 8)
                setImageClickListener(image, binding.imageView)
            }else{
                binding.imageView.setImageDrawable(null);
            }
            val replyMessageImage = item.message.replyMessage?.image
            if (replyMessageImage != null) loadImage(replyMessageImage, binding.replyImage, 16)
            val detector = getGestureDetector(binding.messageCardView, item.message)
            binding.messageCardView.setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                false
            }
            binding.layout.visibility = View.VISIBLE
        } else {
            binding.layout.visibility = View.GONE
            binding.message = null
        }
    }

    fun loadDetailedImage(position: Int, viewHolder: RecyclerView.ViewHolder) {
        val binding =
            (viewHolder as ExtPagedListAdapter<MessageView, MessageLayoutBinding>.VH).binding
        val image = getItem(position)?.message?.image
        if (image != null) loadImage(image, binding.imageView, 2)
    }

    private fun loadImage(image: String, imageView: ImageView, quality: Int) {
        val file = File(getFilesDir(imageView.context), image)
        if (file.exists()) {
            imageView.setImageBitmap(
                BitmapFactory.decodeFile(
                    file.path,
                    BitmapFactory.Options().apply { inSampleSize = quality }
                )
            )
        }
    }

    private fun getGestureDetector(messageCardView: MaterialCardView, message: Message) =
        GestureDetectorCompat(
            messageCardView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent) = true

                override fun onFling(
                    event1: MotionEvent?,
                    event2: MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    messageCardView.animate().translationX(windowWidth.toFloat()).setDuration(500)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(var1: Animator) {
                                messageCardView.translationX = 0f
                                onReply(message)
                            }
                        }).setInterpolator(AccelerateInterpolator()).start()
                    return true
                }
            })

    private fun setImageClickListener(image: String, imageView: ImageView) {
        imageView.setOnClickListener {
            val navController = Navigation.findNavController(imageView)
            val extras = FragmentNavigator.Extras.Builder()
                .addSharedElement(imageView, imageView.transitionName)
                .build()
            val bundle = Bundle().apply { putString("image", image) }
            navController.navigate(R.id.imageViewFragment, bundle, null, extras)
        }
    }
}