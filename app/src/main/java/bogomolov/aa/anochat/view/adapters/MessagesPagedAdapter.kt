package bogomolov.aa.anochat.view.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import androidx.core.view.GestureDetectorCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.paging.PagedListAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.databinding.MessageLayoutBinding
import bogomolov.aa.anochat.view.MessageView
import java.io.File

class MessagesPagedAdapter(
    private val activity: Activity,
    private val onReply: (Message) -> Unit,
    private val helper: AdapterHelper<MessageView, MessageLayoutBinding> = AdapterHelper(),
    private val setRecyclerViewState: () -> Unit
) :
    PagedListAdapter<MessageView, AdapterHelper<MessageView, MessageLayoutBinding>.VH>(
        helper.DIFF_CALLBACK
    ),
    AdapterSelectable<MessageView, MessageLayoutBinding> {

    init {
        helper.adapter = this
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterHelper<MessageView, MessageLayoutBinding>.VH {
        val binding =
            MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return helper.VH(binding.root, binding.messageCardView, binding)
    }

    override fun onBindViewHolder(
        holder: AdapterHelper<MessageView, MessageLayoutBinding>.VH,
        position: Int
    ) = helper.onBindViewHolder(holder, position)

    override fun getItem(position: Int) = super.getItem(position)

    override fun getId(item: MessageView) = item.message.id

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(item: MessageView?, binding: MessageLayoutBinding) {
        if (item != null) {
            binding.message = item
            if (item.hasImage()) {
                //Char.isSurrogate()
                val file = File(getFilesDir(activity), item.message.image!!)
                if (file.exists()) {
                    binding.imageView.setImageBitmap(
                        BitmapFactory.decodeFile(
                            file.path,
                            BitmapFactory.Options().apply { inSampleSize = 1 }
                        )
                    )
                    binding.imageView.setOnClickListener {
                        val navController =
                            Navigation.findNavController(activity, R.id.nav_host_fragment)
                        val extras = FragmentNavigator.Extras.Builder()
                            .addSharedElement(binding.imageView, binding.imageView.transitionName)
                            .build()
                        setRecyclerViewState()
                        val bundle = Bundle().apply { putString("image", file.name) }
                        navController.navigate(R.id.imageViewFragment, bundle, null, extras)
                    }
                }
            }
            if (item.hasReplyMessageImage()) {
                val file = File(getFilesDir(activity), item.message.replyMessage?.image!!)
                if (file.exists())
                    binding.replyImage.setImageBitmap(BitmapFactory.decodeFile(file.path))
            }

            val detector =
                GestureDetectorCompat(activity, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(event: MotionEvent): Boolean {
                        Log.d("test", "onDown: $event")
                        return true
                    }

                    override fun onFling(
                        event1: MotionEvent,
                        event2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        Log.d(
                            "test",
                            "onFling: $event1 $event2 velocityX $velocityX velocityY $velocityY"
                        )
                        val displayMetrics = DisplayMetrics()
                        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                        val length = displayMetrics.widthPixels.toFloat()
                        binding.messageCardView.animate().translationX(length).setDuration(500)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(var1: Animator) {
                                    Log.i("test", "onAnimationEnd")
                                    binding.messageCardView.translationX = 0f
                                    onReply(item.message)
                                }
                            }).setInterpolator(AccelerateInterpolator()).start()
                        return true
                    }
                })
            binding.messageCardView.setOnTouchListener { view, event ->
                detector.onTouchEvent(event)
                true
            }
            binding.layout.visibility = View.VISIBLE
        }else{
            binding.layout.visibility = View.GONE
        }


    }

}
