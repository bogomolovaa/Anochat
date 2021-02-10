package bogomolov.aa.anochat.features.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentMiniatureBinding
import bogomolov.aa.anochat.features.settings.actions.UpdatePhotoAction
import bogomolov.aa.anochat.repository.getFilesDir
import dagger.android.support.AndroidSupportInjection
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class MiniatureFragment : Fragment(), View.OnTouchListener {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: SettingsViewModel by navGraphViewModels(R.id.settings_graph) { viewModelFactory }
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
    ): View {
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

        binding.fab.setOnClickListener {
            val maskWidth = binding.maskImage.scaledWidth()
            val maskHeight = binding.maskImage.scaledHeight()
            viewModel.addAction(
                UpdatePhotoAction(
                    photo = imageName,
                    x = (maskX / initialImageScale).toInt(),
                    y = (maskY / initialImageScale).toInt(),
                    width = (maskWidth / initialImageScale).toInt(),
                    height = (maskHeight / initialImageScale).toInt()
                )
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


    private var lastPoint: Point = Point()
    private var relativeLayout: RelativeLayout? = null
    private var scaleFactor = 1f
    private var imageWidth = 0
    private var imageHeight = 0
    private var maxScale = 1f
    private var initialImageScale = 1f
    private var maskX = 0
    private var maskY = 0

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor

            scaleFactor = max(0.5f, min(scaleFactor, maxScale))
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
                layoutParams.rightMargin =
                    relativeLayout!!.measuredWidth - layoutParams.leftMargin + view.scaledWidth()
                layoutParams.bottomMargin =
                    relativeLayout!!.measuredHeight - layoutParams.topMargin + view.scaledHeight()

                val dx = (view.scaledWidth() - view.width) / 2
                val dy = (view.scaledHeight() - view.height) / 2
                if (layoutParams.topMargin < dy) layoutParams.topMargin = dy
                if (layoutParams.leftMargin < dx) layoutParams.leftMargin = dx

                if (imageWidth == 0 && imageHeight == 0) setImageRealDimensions()

                if (layoutParams.leftMargin + view.width + dx > imageWidth)
                    layoutParams.leftMargin = imageWidth - view.width - dx
                if (layoutParams.topMargin + view.height + dy > imageHeight)
                    layoutParams.topMargin = imageHeight - view.height - dy

                maskX = layoutParams.leftMargin - dx
                maskY = layoutParams.topMargin - dy

                view.layoutParams = layoutParams
                lastPoint = point;
            }
        }
        return true
    }

    private fun View.scaledWidth(): Int = (scaleFactor * this.width).toInt()
    private fun View.scaledHeight(): Int = (scaleFactor * this.height).toInt()

}
