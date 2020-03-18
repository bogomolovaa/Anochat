package bogomolov.aa.anochat.view.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentMiniatureBinding
import bogomolov.aa.anochat.viewmodel.SettingsViewModel
import dagger.android.support.AndroidSupportInjection
import java.io.File
import javax.inject.Inject
import kotlin.math.max

class MiniatureFragment : Fragment(), View.OnTouchListener {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: SettingsViewModel by activityViewModels { viewModelFactory }
    private lateinit var navController: NavController
    private lateinit var binding: FragmentMiniatureBinding
    private lateinit var bitmap: Bitmap

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_miniature,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        val imageName = arguments?.getString("image")!!
        val imagePath = File(getFilesDir(requireContext()), imageName).path

        bitmap = BitmapFactory.decodeFile(imagePath)
        binding.imageView.setImageBitmap(bitmap)
        Log.i("Test", "bitmap (${bitmap.width}, ${bitmap.height})")

        binding.fab.setOnClickListener {
            val maskWidth = binding.maskImage.measuredWidth
            val maskHeight = binding.maskImage.measuredHeight
            viewModel.updatePhoto(
                photo = imageName,
                x = (maskX / initialImageScale).toInt(),
                y = (maskY / initialImageScale).toInt(),
                width = (maskWidth / initialImageScale).toInt(),
                height = (maskHeight / initialImageScale).toInt()
            )
            navController.navigateUp()
        }

        relativeLayout = binding.layout
        scaleDetector = ScaleGestureDetector(context, scaleListener)
        binding.maskImage.setOnTouchListener(this)
        binding.layout.setOnTouchListener { v, event ->
            scaleDetector.onTouchEvent(event)
        }



        return binding.root
    }


    var lastPoint: Point = Point()
    var relativeLayout: RelativeLayout? = null
    private var scaleFactor = 1f
    var imageWidth = 0
    var imageHeight = 0
    var maxScale = 1f
    var initialImageScale = 1f
    var maskX = 0
    var maskY = 0

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, maxScale))
            Log.i("test", "scaleFactor $scaleFactor maxScale $maxScale")

            Log.i("test", "setScale $scaleFactor")
            binding.maskImage.scaleX = scaleFactor
            binding.maskImage.scaleY = scaleFactor

            return true
        }
    }
    private lateinit var scaleDetector: ScaleGestureDetector

    private fun setImageRealDimensions() {
        val koef1 = bitmap.width.toFloat() / bitmap.height.toFloat()
        val koef2 =
            binding.imageView.measuredWidth.toFloat() / binding.imageView.measuredHeight.toFloat()
        if (koef1 >= koef2) {
            imageWidth = binding.imageView.measuredWidth
            imageHeight = (binding.imageView.measuredWidth / koef1).toInt()
            initialImageScale = imageWidth.toFloat() / bitmap.width.toFloat()
        } else {
            imageWidth = (koef1 * binding.imageView.measuredHeight).toInt()
            imageHeight = binding.imageView.measuredHeight
            initialImageScale = imageHeight.toFloat() / bitmap.height.toFloat()
        }
        val maxScaleX = imageWidth.toFloat() / binding.maskImage.measuredWidth.toFloat()
        val maxScaleY = imageHeight.toFloat() / binding.maskImage.measuredHeight.toFloat()
        maxScale = max(maxScaleX, maxScaleY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val point = Point(event.rawX.toInt(), event.rawY.toInt())
        scaleDetector.onTouchEvent(event)


        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint = point
            }
            MotionEvent.ACTION_MOVE -> {
                val offset = Point(point.x - lastPoint.x, point.y - lastPoint.y)
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                layoutParams.leftMargin += offset.x
                layoutParams.topMargin += offset.y
                layoutParams.rightMargin =
                    relativeLayout!!.measuredWidth - layoutParams.leftMargin + view.measuredWidth
                layoutParams.bottomMargin =
                    relativeLayout!!.measuredHeight - layoutParams.topMargin + view.measuredHeight

                if (layoutParams.topMargin < 0) layoutParams.topMargin = 0
                if (layoutParams.leftMargin < 0) layoutParams.leftMargin = 0

                if (imageWidth == 0 && imageHeight == 0) setImageRealDimensions()

                if (layoutParams.leftMargin + view.measuredWidth > imageWidth)
                    layoutParams.leftMargin = imageWidth - view.measuredWidth
                if (layoutParams.topMargin + view.measuredHeight > imageHeight)
                    layoutParams.topMargin = imageHeight - view.measuredHeight

                maskX = layoutParams.leftMargin
                maskY = layoutParams.topMargin

                view.layoutParams = layoutParams
                lastPoint = point;
            }
        }
        return true
    }


}
