package bogomolov.aa.anochat.view.fragments

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Point
import androidx.transition.TransitionInflater
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.databinding.FragmentImageViewBinding
import bogomolov.aa.anochat.view.MainActivity
import java.io.File
import kotlin.math.max
import kotlin.math.min


class ImageViewFragment : Fragment(), View.OnTouchListener  {
    private lateinit var binding: FragmentImageViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        sharedElementReturnTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_image_view,
            container,
            false
        )
        val mainActivity = activity as MainActivity
        mainActivity.setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar?.title = ""


        var expanded = false
        val imageName = arguments?.getString("image")!!
        val imagePath = File(getFilesDir(requireContext()), imageName).path
        binding.imageView.transitionName = imageName
        binding.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        binding.imageView.setOnClickListener {
            expanded = !expanded
            binding.toolbar.visibility = if (expanded) View.INVISIBLE else View.VISIBLE
        }

        scaleDetector = ScaleGestureDetector(context, scaleListener)
        return binding.root
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor

            scaleFactor = max(1f, min(scaleFactor, MAX_SCALE))
            binding.imageView.scaleX = scaleFactor
            binding.imageView.scaleY = scaleFactor

            return true
        }
    }
    private lateinit var scaleDetector: ScaleGestureDetector
    var layout: ConstraintLayout? = null
    private var scaleFactor = 1f
    var lastPoint: Point = Point()
    companion object{
        private const val MAX_SCALE = 1f
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val point = Point(event.rawX.toInt(), event.rawY.toInt())


        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint = point
            }
            MotionEvent.ACTION_MOVE -> {
                val offset = Point(point.x - lastPoint.x, point.y - lastPoint.y)
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                layoutParams.leftMargin += offset.x
                layoutParams.topMargin += offset.y
                
                /*
                layoutParams.rightMargin =
                layout!!.measuredWidth - layoutParams.leftMargin + view.scaledWidth()
                layoutParams.bottomMargin =
                layout!!.measuredHeight - layoutParams.topMargin + view.scaledHeight()
                val dx = (view.scaledWidth() - view.width) / 2
                val dy = (view.scaledHeight() - view.height) / 2
                if (layoutParams.topMargin < dy) layoutParams.topMargin = dy
                if (layoutParams.leftMargin < dx) layoutParams.leftMargin = dx

                if (layoutParams.leftMargin + view.width + dx > layout!!.measuredWidth)
                    layoutParams.leftMargin = layout!!.measuredWidth - view.width - dx
                if (layoutParams.topMargin + view.height + dy > layout!!.measuredHeight)
                    layoutParams.topMargin = layout!!.measuredHeight - view.height - dy
                */

                view.layoutParams = layoutParams
                lastPoint = point;
            }
        }
        return true
    }

    private fun View.scaledWidth(): Int = (scaleFactor * this.width).toInt()
    private fun View.scaledHeight(): Int = (scaleFactor * this.width).toInt()

}
