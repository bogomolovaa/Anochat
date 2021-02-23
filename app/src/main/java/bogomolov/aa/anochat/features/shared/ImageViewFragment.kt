package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
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
    private var scale = 1f
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
        scaleDetector = ScaleGestureDetector(context, scaleListener)
        binding.touchLayout.setOnTouchListener(imageOnTouchListener)

        systemUiVisibility = requireActivity().window.decorView.systemUiVisibility
        //showSystemUI()

        return binding.root
    }

    private var scaling = false

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaling = true
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            scaling = false
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val width = binding.imageView.width
            val height = binding.imageView.height
            val prevScale = scale
            val imageView = binding.imageView
            scale *= detector.scaleFactor
            scale = max(MIN_SCALE, min(scale, MAX_SCALE))
            imageView.scaleX = scale
            imageView.scaleY = scale

            val deltaWidth = (scale - prevScale) * width
            val deltaHeight = (scale - prevScale) * height

            val focusX = detector.focusX + (scale - 1) * width / 2 - imageView.translationX
            val focusY = detector.focusY + (scale - 1) * height / 2 - imageView.translationY
            imageView.translationX += (deltaWidth / 2) * (1 - focusX / (scale * width / 2))
            imageView.translationY += (deltaHeight / 2) * (1 - focusY / (scale * height / 2))

            checkBounds()
            return true
        }
    }

    private fun checkBounds() {
        val imageView = binding.imageView
        if (imageView.translationX > (scale - 1) * imageView.width / 2)
            imageView.translationX = (scale - 1) * imageView.width / 2
        if (imageView.translationX < -(scale - 1) * imageView.width / 2)
            imageView.translationX = -(scale - 1) * imageView.width / 2

        val height = imageView.width * bitmap!!.height / bitmap!!.width
        if (imageView.translationY > (scale - 1) * height / 2)
            imageView.translationY = (scale - 1) * height / 2
        if (imageView.translationY < -(scale - 1) * height / 2)
            imageView.translationY = -(scale - 1) * height / 2
    }

    private val imageOnTouchListener = object : View.OnTouchListener {
        private var canMove = true
        private var lastPoint: Point = Point()

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            if (!scaling) {
                val imageView = binding.imageView
                val point = Point(event.rawX.toInt(), event.rawY.toInt())
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        lastPoint = point
                        canMove = true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> canMove = false
                    MotionEvent.ACTION_MOVE -> {
                        if (canMove) {
                            val offset = Point(point.x - lastPoint.x, point.y - lastPoint.y)
                            imageView.translationX += offset.x
                            imageView.translationY += offset.y
                            checkBounds()
                            //expand(true)
                        }
                        lastPoint = point
                    }
                    //MotionEvent.ACTION_UP -> if (!moved) expand(!expanded)
                    MotionEvent.ACTION_UP -> canMove = false
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