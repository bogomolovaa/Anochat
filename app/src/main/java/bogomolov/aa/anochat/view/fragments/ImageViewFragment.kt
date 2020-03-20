package bogomolov.aa.anochat.view.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
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


class ImageViewFragment : Fragment(), View.OnTouchListener {
    private lateinit var binding: FragmentImageViewBinding
    private lateinit var bitmap: Bitmap
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
        bitmap = BitmapFactory.decodeFile(imagePath)
        binding.imageView.setImageBitmap(bitmap)
        binding.imageView.setOnClickListener {
            expanded = !expanded
            binding.toolbar.visibility = if (expanded) View.INVISIBLE else View.VISIBLE
        }

        scaleDetector = ScaleGestureDetector(context, scaleListener)
        binding.imageView.setOnTouchListener(this)
        return binding.root
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor

            scaleFactor = max(minScale, min(scaleFactor, maxScale))
            binding.imageView.scaleX = scaleFactor
            binding.imageView.scaleY = scaleFactor

            return true
        }
    }
    private lateinit var scaleDetector: ScaleGestureDetector
    private var scaleFactor = 1f
    private var lastPoint: Point = Point()
    private var maxScale = 2f
    private var minScale = 1f

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
                view.translationX += offset.x
                view.translationY += offset.y
                val dx = (view.scaledWidth() - view.width) / 2
                val dy = (view.scaledHeight() - view.height) / 2

                if (view.translationX < -dx) view.translationX = -dx.toFloat()
                if (view.translationY < -dy) view.translationY = -dy.toFloat()

                if (view.translationX > dx) view.translationX = dx.toFloat()
                if (view.translationY > dx) view.translationY = dy.toFloat()

                lastPoint = point;
            }
        }
        return true
    }

    private fun View.scaledWidth(): Int = (scaleFactor * this.width).toInt()
    private fun View.scaledHeight(): Int = (scaleFactor * this.width).toInt()

}
