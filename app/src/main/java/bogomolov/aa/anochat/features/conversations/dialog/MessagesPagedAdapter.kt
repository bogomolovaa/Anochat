package bogomolov.aa.anochat.features.conversations.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
            val context = binding.root.context
            if (item.hasReplyMessage()) {
                binding.replyLayout.visibility = View.VISIBLE
                val padding = if (item.getReplyText().isNotEmpty())
                    resDimension(R.dimen.reply_message_text_padding, context).toInt()
                else 0
                binding.replyText.setPadding(padding, 0, padding, 0)
                binding.replyText.text = item.getReplyText()
            } else {
                binding.replyLayout.visibility = View.GONE
            }
            binding.statusLayout.visibility = if (item.message.isMine) View.VISIBLE else View.GONE
            binding.notSentStatus.visibility = if (!item.sent()) View.VISIBLE else View.GONE
            binding.sentAndNotReceivedStatus.visibility =
                if (item.sentAndNotReceived()) View.VISIBLE else View.GONE
            binding.receivedAndNotViewedStatus.visibility =
                if (item.receivedAndNotViewed()) View.VISIBLE else View.GONE
            binding.receivedAndViewedStatus.visibility =
                if (item.viewed()) View.VISIBLE else View.GONE
            binding.errorStatus.visibility = if (item.error()) View.VISIBLE else View.GONE
            binding.dateText.text = item.dateDelimiter
            binding.dateCardLayout.visibility =
                if (item.hasTimeMessage()) View.VISIBLE else View.GONE
            binding.messageLayout.gravity = if (item.message.isMine) Gravity.END else Gravity.START
            binding.messageCardView.setCardBackgroundColor(
                if (item.message.isMine) resColor(R.color.my_message_color, context)
                else resColor(R.color.not_my_message_color, context)
            )
            val dim4dp = resDimension(R.dimen.margin_4dp, context)
            val dim64dp = resDimension(R.dimen.margin_64dp, context)
            if (item.message.isMine) {
                setLayoutMargin(binding.messageCardView, dim64dp, dim4dp)
            } else {
                setLayoutMargin(binding.messageCardView, dim4dp, dim64dp)
            }

            binding.imageProgressLayout.visibility = View.GONE
            val detector = getGestureDetector(binding.messageCardView, item.message, holder)
            val text = item.message.text

            if (text.length in 1..2 && Character.isSurrogate(text[0])) {
                binding.messageText.setEmojiSizeRes(R.dimen.message_one_emoji_size)
            } else {
                binding.messageText.setEmojiSizeRes(R.dimen.message_emoji_size)
            }
            if (text.contains("(https|http)".toRegex())) {
                setTextViewHTML(binding.messageText, insertLinks(text))
            } else {
                binding.messageText.text = toSpanned(insertLinks(text))
            }
            binding.messageText.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE

            binding.timeText.text = item.message.shortTimeString()
            binding.timeText.linksClickable = true;
            binding.timeText.movementMethod = LinkMovementMethod.getInstance()
            binding.timeText.setTextColor(Color.BLACK)
            (binding.timeText.layoutParams as ViewGroup.MarginLayoutParams).rightMargin =
                if (item.message.isMine) 0 else dim4dp.toInt()

            val image = item.message.image
            if (!image.isNullOrEmpty()) {
                item.detailedImageLoaded = false
                if (!loadImage(image, binding.imageView, 8))
                    showImageNotLoaded(binding, item.message.time)
                if (text.isEmpty()) binding.timeText.setTextColor(Color.WHITE)
                setImageClickListener(item.message, binding.imageView, detector)
                binding.imageView.transitionName = item.message.image

                binding.messageCardView.layoutParams.width =
                    resDimension(R.dimen.message_image_width, context).toInt()
            } else {
                binding.messageCardView.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.imageView.setImageDrawable(null)
                binding.imageView.visibility = View.GONE
            }
            val replyMessageImage = item.message.replyMessage?.image
            if (replyMessageImage != null) {
                val bitmap = getBitmapFromGallery(replyMessageImage, context, 16)
                binding.replyImage.setImageBitmap(bitmap)
                binding.replyImage.visibility = View.VISIBLE
                if (item.getReplyText().isNotEmpty()) {
                    val paddingLeft =
                        resDimension(R.dimen.reply_message_text_padding, context).toInt()
                    val paddingRight = paddingLeft +
                            resDimension(R.dimen.reply_message_image_width, context).toInt()
                    binding.replyText.setPadding(paddingLeft, 0, paddingRight, 0)
                }
            } else {
                binding.replyText.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.replyImage.visibility = View.GONE
            }
            binding.messageCardView.setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
            }
            initPlayAudioView(item.message, binding.playAudioInput, messagesMap)
            initPlayAudioView(item.message.replyMessage, binding.replayAudio, replyMessagesMap)
        }
    }

    private fun setLayoutMargin(view: MaterialCardView, marginLeft: Float, marginRight: Float) {
        val p = view.layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(marginLeft.toInt(), p.topMargin, marginRight.toInt(), p.bottomMargin)
    }

    private fun resColor(id: Int, context: Context) = ContextCompat.getColor(context, id)
    private fun resDimension(id: Int, context: Context) = context.resources.getDimension(id)

    override fun onItemSelected(binding: MessageLayoutBinding, selected: Boolean) {
        val context = binding.root.context
        val color = if (selected)
            ContextCompat.getColor(context, R.color.my_message_color)
        else
            ContextCompat.getColor(context, R.color.conversation_background)
        binding.messageLayout.setBackgroundColor(color)
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
            audioView.visibility = View.VISIBLE
        } else {
            audioView.visibility = View.GONE
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
        } else {
            imageView.setImageDrawable(null)
            imageView.visibility = View.GONE
            imageView.requestLayout()
        }
        return false
    }

    private fun showImageNotLoaded(binding: MessageLayoutBinding, receivedTime: Long) {
        binding.imageView.visibility = View.GONE
        binding.imageProgressLayout.visibility = View.VISIBLE
        val elapsed = System.currentTimeMillis() - receivedTime
        if (elapsed / 1000 < WAITING_IMAGE_TIMEOUT) {
            binding.imageProgressBar.visibility = View.VISIBLE
            binding.errorLoadingImage.visibility = View.INVISIBLE
        } else {
            binding.errorLoadingImage.visibility = View.VISIBLE
            binding.imageProgressBar.visibility = View.INVISIBLE
        }
    }

    private fun getDpPixels(context: Context) = context.resources.displayMetrics.density

    private fun setImageSize(imageView: ImageView, bitmap: Bitmap) {
        val width = (250 * getDpPixels(imageView.context)).toInt()
        val ratio = 1f * bitmap.height / bitmap.width
        val height = (ratio * width).toInt()
        val savedParams = imageView.layoutParams as LinearLayout.LayoutParams
        val layoutParams = LinearLayout.LayoutParams(width, min(height, width))
        layoutParams.leftMargin = savedParams.leftMargin
        layoutParams.rightMargin = savedParams.rightMargin
        layoutParams.topMargin = savedParams.topMargin
        imageView.layoutParams = layoutParams
    }

    private fun getGestureDetector(
        messageCardView: MaterialCardView,
        message: Message,
        holder: VH
    ) =
        GestureDetectorCompat(
            messageCardView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent) = true

                override fun onLongPress(e: MotionEvent?) {
                    holder.onLongClick()
                }

                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                    holder.onClick()
                    return false
                }

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

    @SuppressLint("ClickableViewAccessibility")
    private fun setImageClickListener(
        message: Message,
        imageView: ImageView,
        detector: GestureDetectorCompat
    ) {
        imageView.setOnTouchListener { v, event ->
            if (!detector.onTouchEvent(event))
                if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP && !selectionMode) {
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
                }
            true
        }

    }

    private fun toSpanned(html: String?): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    private fun insertLinks(text: String) =
        text.replace("(https|http)://[^ ]+".toRegex(), """<a href="$0">$0</a>""")

    private fun setTextViewHTML(textView: TextView, html: String?) {
        val sequence: CharSequence = toSpanned(html)
        val strBuilder = SpannableStringBuilder(sequence)
        val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
        for (span in urls) {
            makeLinkClickable(textView, strBuilder, span)
        }
        textView.text = strBuilder
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun makeLinkClickable(
        textView: TextView,
        strBuilder: SpannableStringBuilder,
        span: URLSpan?
    ) {
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        val clickable = object : ClickableSpan() {
            override fun onClick(widget: View) {
                span?.onClick(textView)
            }
        }
        strBuilder.setSpan(clickable, start, end, flags)
        strBuilder.removeSpan(span)
    }
}