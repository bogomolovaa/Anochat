package bogomolov.aa.anochat.features.conversations.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.MessageLayoutBinding
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.ActionModeData
import bogomolov.aa.anochat.features.shared.ExtPagedListAdapter
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.mvi.ActionExecutor
import com.google.android.material.card.MaterialCardView
import kotlin.math.min

private const val WAITING_IMAGE_TIMEOUT = 180

class MessagesPagedAdapter(
    private val windowWidth: Int,
    private val onReply: (Message) -> Unit,
    private val actionExecutor: ActionExecutor,
    actionModeData: ActionModeData<MessageView>? = null,
) : ExtPagedListAdapter<MessageView, MessageLayoutBinding>(actionModeData) {
    val messagesMap = HashMap<String, PlayAudioView>()
    val replyMessagesMap = HashMap<String, PlayAudioView>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding.root, binding)
    }

    override fun getId(item: MessageView) = item.message.id

    override fun getItemId(position: Int) = getItem(position)?.message?.id ?: 0

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(item: MessageView?, holder: VH) {
        val binding = holder.binding
        if (item != null) {
            //Char.isSurrogate()
            val context = binding.root.context
            binding.message = item
            binding.imageProgressLayout.visibility = View.GONE
            val image = item.message.image
            if (!image.isNullOrEmpty()) {
                item.detailedImageLoaded = false
                if (!loadImage(image, binding.imageView, 8))
                    showImageNotLoaded(binding, item.message.time)
                setImageClickListener(item.message, binding.imageView, holder)
            }
            val replyMessageImage = item.message.replyMessage?.image
            if (replyMessageImage != null) {
                val bitmap = getBitmapFromGallery(replyMessageImage, context, 16)
                binding.replyImage.setImageBitmap(bitmap)
            }
            val detector = getGestureDetector(binding.messageCardView, item.message)
            binding.messageCardView.setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                false
            }
            initPlayAudioView(item.message, binding.playAudioInput, messagesMap)
            initPlayAudioView(item.message.replyMessage, binding.replayAudio, replyMessagesMap)

            val color = if (isChecked(item))
                ContextCompat.getColor(context, R.color.my_message_color)
            else
                ContextCompat.getColor(context, R.color.conversation_background)
            binding.messageLayout.setBackgroundColor(color)
            binding.executePendingBindings()
        } else {
            binding.message = null
        }
    }

    private fun initPlayAudioView(
        message: Message?,
        audioView: PlayAudioView,
        map: HashMap<String, PlayAudioView>
    ) {
        if (message?.audio != null) {
            val audio = if (message.received == 1 || message.isMine) message.audio else null
            audioView.set(audio, message.messageId)
            audioView.actionExecutor = actionExecutor
            map[message.messageId] = audioView
        }
    }

    fun loadDetailedImage(position: Int, viewHolder: RecyclerView.ViewHolder) {
        val binding =
            (viewHolder as ExtPagedListAdapter<MessageView, MessageLayoutBinding>.VH).binding
        val item = getItem(position)
        if (item != null) {
            val image = item.message.image
            if (!image.isNullOrEmpty() && !item.detailedImageLoaded) {
                item.detailedImageLoaded = true
                if (!loadImage(image, binding.imageView, 2))
                    showImageNotLoaded(binding, item.message.time)
            }
        }
    }

    private fun loadImage(image: String, imageView: ImageView, quality: Int): Boolean {
        val bitmap = getBitmapFromGallery(image, imageView.context, quality)
        if (bitmap != null) {
            imageView.visibility = View.VISIBLE
            imageView.setImageBitmap(bitmap)
            setImageSize(imageView, bitmap)
            return true
        }
        return false
    }

    private fun showImageNotLoaded(binding: MessageLayoutBinding, receivedTime: Long) {
        binding.imageView.visibility = View.GONE
        binding.imageProgressLayout.visibility = View.VISIBLE
        val elapsed = System.currentTimeMillis() - receivedTime
        if (elapsed / 1000 < WAITING_IMAGE_TIMEOUT) {
            binding.imageProgressBar.visibility = View.VISIBLE
        } else {
            binding.errorLoadingImage.visibility = View.VISIBLE
        }
    }

    private fun setImageSize(imageView: ImageView, bitmap: Bitmap) {
        val density = imageView.context.resources.displayMetrics.density
        val width = (250 * density).toInt()
        val ratio = 1f * bitmap.height / bitmap.width
        val height = (ratio * width).toInt()
        val savedParams = imageView.layoutParams as LinearLayout.LayoutParams
        val layoutParams = LinearLayout.LayoutParams(width, min(height, width))
        layoutParams.leftMargin = savedParams.leftMargin
        layoutParams.rightMargin = savedParams.rightMargin
        layoutParams.topMargin = savedParams.topMargin
        imageView.layoutParams = layoutParams
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

    private fun setImageClickListener(message: Message, imageView: ImageView, holder: VH) {
        imageView.setOnClickListener {
            if (!selectionMode) {
                if (message.received == 1 || message.isMine) {
                    val navController = Navigation.findNavController(imageView)
                    val extras = FragmentNavigator.Extras.Builder()
                        .addSharedElement(imageView, imageView.transitionName)
                        .build()
                    val bundle = Bundle().apply {
                        putString("image", message.image)
                        putInt("quality", 2)
                        putBoolean("gallery", true)
                    }
                    navController.navigate(R.id.imageViewFragment, bundle, null, extras)
                }
            } else {
                holder.onClick()
            }
        }
        imageView.setOnLongClickListener {
            holder.onLongClick()
            true
        }
    }
}