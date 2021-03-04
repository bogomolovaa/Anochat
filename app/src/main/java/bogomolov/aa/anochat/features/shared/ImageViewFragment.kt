package bogomolov.aa.anochat.features.shared

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionListenerAdapter
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentImageViewBinding
import bogomolov.aa.anochat.features.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


private const val MAX_SCALE = 10f
private const val MIN_SCALE = 1f
private const val TAG = "ImageViewFragment"

class ImageViewFragment : Fragment() {
    private lateinit var binding: FragmentImageViewBinding
    private var bitmap: Bitmap? = null
    private lateinit var mainActivity: MainActivity
    private lateinit var scaleDetector: ScaleGestureDetector
    private var savedSystemUiVisibility = 0
    private var scale = 1f
    private var expanded = false
    private lateinit var imageName: String
    private var fromGallery = false
    private var quality = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedSystemUiVisibility = requireActivity().window.decorView.systemUiVisibility
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.change_image_transform)

        requireActivity().window.decorView.setBackgroundResource(R.color.black)
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.black)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
    }

    private val onTransitionEndListener = object : TransitionListenerAdapter() {
        override fun onTransitionEnd(transition: Transition) {
            if (quality > 1) loadImage(1)
        }
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
        setHasOptionsMenu(true)

        imageName = arguments?.getString("image")!!
        quality = arguments?.getInt("quality")!!
        fromGallery = arguments?.getBoolean("gallery") ?: false
        binding.imageView.transitionName = imageName
        loadImage(quality)
        scaleDetector = ScaleGestureDetector(context, scaleListener)
        binding.touchLayout.setOnTouchListener(imageOnTouchListener)


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) showSystemUI()

        binding.toolbar.setNavigationOnClickListener { onBackPressed(navController) }
        requireActivity().onBackPressedDispatcher.addCallback(owner = viewLifecycleOwner) {
            onBackPressed(navController)
        }

        binding.imageView.post {
            windowHeight = requireActivity().window.decorView.height
        }

        (sharedElementEnterTransition as Transition).addListener(onTransitionEndListener)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.image_view_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_share) {
            val uri = getUri(imageName, requireContext())
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                val title = resources.getString(R.string.share_image)
                startActivity(Intent.createChooser(intent, title))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onBackPressed(navController: NavController) {
        if (quality > 1) loadImage(quality)
        navController.navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.decorView.systemUiVisibility = savedSystemUiVisibility
        (sharedElementEnterTransition as Transition).removeListener(onTransitionEndListener)
    }

    private fun loadImage(quality: Int) {
        try {
            bitmap = if (fromGallery) getBitmapFromGallery(imageName, requireContext(), quality)
            else getBitmap(imageName, requireContext(), quality)
            binding.imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "image not loaded", e)
        }
    }

    private var scaling = false
    private var canExpand = true
    private var windowHeight = 0


    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaling = true
            canExpand = false
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

            checkBounds(0, 0)
            return true
        }
    }

    private fun checkBounds(delta1: Int, delta2: Int) {
        val imageView = binding.imageView
        val maxShiftX = (scale - 1) * imageView.width / 2
        if (imageView.translationX > maxShiftX)
            imageView.translationX = maxShiftX
        if (imageView.translationX < -maxShiftX)
            imageView.translationX = -maxShiftX

        val maxShiftY =
            ((scale - 1) * imageView.height - (scale) * delta1) / 2 - delta2
        if (imageView.translationY > maxShiftY)
            imageView.translationY = maxShiftY
        if (imageView.translationY < -maxShiftY)
            imageView.translationY = -maxShiftY
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
                        canExpand = true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> canMove = false
                    MotionEvent.ACTION_MOVE -> {
                        if (canMove) {
                            val offset = Point(point.x - lastPoint.x, point.y - lastPoint.y)
                            imageView.translationX += offset.x
                            val realHeight = imageView.width * bitmap!!.height / bitmap!!.width
                            if (scale * realHeight > windowHeight) {
                                imageView.translationY += offset.y
                                checkBounds(
                                    imageView.height - realHeight,
                                    windowHeight - imageView.height
                                )
                            } else {
                                checkBounds(0, 0)
                            }
                            canExpand = false
                        }
                        lastPoint = point
                    }
                    MotionEvent.ACTION_UP -> {
                        canMove = false
                        if (canExpand) expand(!expanded)
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

    private fun hideSystemUI() {
        requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    private fun showSystemUI() {
        requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}