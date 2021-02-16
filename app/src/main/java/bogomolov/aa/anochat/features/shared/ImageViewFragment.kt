package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.transition.TransitionInflater
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentImageViewBinding
import bogomolov.aa.anochat.features.main.MainActivity
import bogomolov.aa.anochat.repository.getFilesDir
import java.io.File
import kotlin.math.max
import kotlin.math.min


class ImageViewFragment : Fragment(), View.OnTouchListener {
    private lateinit var binding: FragmentImageViewBinding
    private lateinit var bitmap: Bitmap
    private lateinit var mainActivity: MainActivity
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
    ): View {
        binding = FragmentImageViewBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        mainActivity.setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar?.title = ""


        val imageName = arguments?.getString("image")!!
        val imagePath = File(getFilesDir(requireContext()), imageName).path
        binding.imageView.transitionName = imageName
        bitmap = BitmapFactory.decodeFile(imagePath)
        binding.imageView.setImageBitmap(bitmap)
        binding.imageView.setOnClickListener { }

        scaleDetector = ScaleGestureDetector(context, scaleListener)
        binding.imageView.setOnTouchListener(this)

        systemUiVisibility = requireActivity().window.decorView.systemUiVisibility
        showSystemUI()

        return binding.root
    }

    private var systemUiVisibility = 0

    override fun onPause() {
        super.onPause()
        requireActivity().window.decorView.systemUiVisibility = systemUiVisibility
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
    private var expanded = false
    private lateinit var scaleDetector: ScaleGestureDetector
    private var scaleFactor = 1f
    private var lastPoint: Point = Point()
    private var maxScale = 2f
    private var minScale = 1f
    private var moved = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val point = Point(event.rawX.toInt(), event.rawY.toInt())


        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint = point
                moved = false
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
                if (view.translationY > dy) view.translationY = dy.toFloat()


                moved = true

                lastPoint = point
                expand(true)
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) expand(!expanded)
            }
        }
        return false
    }

    private fun dxdy(view: View): Point {
        val dx = (view.scaledWidth() - view.width) / 2
        val dy = (view.scaledHeight() - view.height) / 2
        return Point(dx, dy)
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
