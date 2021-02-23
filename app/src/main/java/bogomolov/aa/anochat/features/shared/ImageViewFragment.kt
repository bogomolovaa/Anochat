package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.transition.TransitionInflater
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentImageViewBinding
import bogomolov.aa.anochat.features.main.MainActivity
import kotlin.math.max
import kotlin.math.min

private const val MAX_SCALE = 10f
private const val MIN_SCALE = 1f

class ImageViewFragment : Fragment() {
    private lateinit var binding: FragmentImageViewBinding
    private var bitmap: Bitmap? = null
    private lateinit var mainActivity: MainActivity
    private lateinit var scaleDetector: ScaleGestureDetector
    private var systemUiVisibility = 0
    private var scaleFactor = 1f
    private var expanded = false

    override fun onPause() {
        super.onPause()
        requireActivity().window.decorView.systemUiVisibility = systemUiVisibility
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition =
            TransitionInflater.from(context)
                .inflateTransition(R.transition.image_view_exit_transition)
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.change_image_transform)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageViewBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        mainActivity.setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar?.title = ""

        val imageName = arguments?.getString("image")!!
        binding.imageView.transitionName = imageName
        bitmap = getBitmap(imageName, requireContext())
        binding.imageView.setImageBitmap(bitmap)
        binding.imageView.setOnClickListener { }
        scaleDetector = ScaleGestureDetector(context, scaleListener)
        binding.touchLayout.setOnClickListener { }
        binding.touchLayout.setOnTouchListener(imageOnTouchListener)
        /*
        binding.imageView4.setOnTouchListener { _, event ->
            Log.i("test","imageView4 onTouch")
            scaleDetector.onTouchEvent(event)
            false
        }
        */




        systemUiVisibility = requireActivity().window.decorView.systemUiVisibility
        //showSystemUI()

        return binding.root
    }

    private var scaling = false

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var initWidth = 0
        private var initHeight = 0

        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            if (scaleFactor == 1f) {
                initWidth = binding.imageView.width
                initHeight = binding.imageView.height
            }
            Log.i("test", "onScaleBegin")
            scaling = true
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            scaling = false
            Log.i("test", "onScaleEnd")
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScaleFactor = scaleFactor
            val imageView = binding.imageView
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(MIN_SCALE, min(scaleFactor, MAX_SCALE))
            imageView.scaleX = scaleFactor
            imageView.scaleY = scaleFactor

            val deltaWidth = (scaleFactor - prevScaleFactor) * initWidth
            val deltaHeight = (scaleFactor - prevScaleFactor) * initHeight

            val focusX = detector.focusX
            val focusY = detector.focusY
            imageView.translationX += (deltaWidth / 2) * ((initWidth / 2 - focusX) / (initWidth / 2))
            imageView.translationY += (deltaHeight / 2) * ((initHeight / 2 - focusY) / (initHeight / 2))

            Log.i(
                "test",
                "scaleFactor $scaleFactor detector ${detector.scaleFactor}"
            )
            return true
        }
    }

    private val imageOnTouchListener = object : View.OnTouchListener {
        private var canMove = true
        private var lastPoint: Point = Point()

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            Log.i("test","onTouch")
            if (!scaling) {
                val imageView = binding.imageView
                val point = Point(event.rawX.toInt(), event.rawY.toInt())
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        lastPoint = point
                        canMove = true
                        //Log.i("test", "ACTION_DOWN canMove $canMove $point")
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        canMove = false
                        //Log.i("test", "ACTION_POINTER_DOWN moved $canMove $lastPoint")
                    }
                    MotionEvent.ACTION_MOVE -> {
                        //Log.i("test", "ACTION_MOVE canMove $canMove $point")
                        if (canMove) {
                            val offset = Point(point.x - lastPoint.x, point.y - lastPoint.y)
                            imageView.translationX += offset.x
                            imageView.translationY += offset.y
                            val dx = (imageView.scaledWidth() - imageView.width) / 2
                            val dy = (imageView.scaledHeight() - imageView.height) / 2

                            if (imageView.translationX < -dx) imageView.translationX = -dx.toFloat()
                            if (imageView.translationY < -dy) imageView.translationY = -dy.toFloat()

                            if (imageView.translationX > dx) imageView.translationX = dx.toFloat()
                            if (imageView.translationY > dy) imageView.translationY = dy.toFloat()
                            //expand(true)
                        }
                        lastPoint = point
                    }
                    //MotionEvent.ACTION_UP -> if (!moved) expand(!expanded)
                    MotionEvent.ACTION_UP -> {
                        canMove = false
                        //Log.i("test", "ACTION_UP canMove $canMove $point")
                    }
                }
            }
            return false
        }
    }

    private fun expand(exp: Boolean) {
        expanded = exp
        if (exp) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun View.scaledWidth(): Int = (scaleFactor * this.width).toInt()
    private fun View.scaledHeight(): Int = (scaleFactor * this.height).toInt()

    private fun hideSystemUI() {
        requireActivity().window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        mainActivity.supportActionBar?.hide()
    }

    private fun showSystemUI() {
        mainActivity.supportActionBar?.show()
        requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}