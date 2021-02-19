package bogomolov.aa.anochat.features.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentMiniatureBinding
import bogomolov.aa.anochat.features.shared.getFilePath
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import dagger.android.support.AndroidSupportInjection
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class MiniatureFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: SettingsViewModel by navGraphViewModels(R.id.settings_graph) { viewModelFactory }
    private lateinit var binding: FragmentMiniatureBinding
    private lateinit var bitmap: Bitmap
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var relativeLayout: RelativeLayout


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMiniatureBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        relativeLayout = binding.layout
        bitmap = viewModel.miniature.bitmap
        binding.imageView.setImageBitmap(bitmap)
        binding.fab.setOnClickListener {
            createMiniature()
            viewModel.addAction(UpdatePhotoAction(viewModel.miniature.name))
            navController.navigateUp()
        }
        binding.maskImage.setOnTouchListener(maskImageOnTouchListener)
        binding.layout.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
        }
        scaleDetector = ScaleGestureDetector(context, scaleListener)

        return binding.root
    }

    private fun createMiniature() {
        val maskWidth = binding.maskImage.scaledWidth()
        val maskHeight = binding.maskImage.scaledHeight()
        val x = (maskX / initialImageScale).toInt()
        val y = (maskY / initialImageScale).toInt()
        val width = (maskWidth / initialImageScale).toInt()
        val height = (maskHeight / initialImageScale).toInt()

        val miniBitmap = Bitmap.createBitmap(
            bitmap,
            max(x, 0),
            max(y, 0),
            min(bitmap.width - Math.max(x, 0), width),
            min(bitmap.height - Math.max(y, 0), height)
        )
        val miniPhotoPath = getFilePath(requireContext(), getMiniPhotoFileName(viewModel.miniature.name))
        miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))
    }


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

    private val maskImageOnTouchListener = object : View.OnTouchListener {
        private var lastPoint: Point = Point()

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            val point = Point(event.rawX.toInt(), event.rawY.toInt())
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> lastPoint = point
                MotionEvent.ACTION_MOVE -> {
                    val offset = Point(point.x - lastPoint.x, point.y - lastPoint.y)
                    val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.leftMargin += offset.x
                    layoutParams.topMargin += offset.y
                    layoutParams.rightMargin =
                        relativeLayout.measuredWidth - layoutParams.leftMargin + view.scaledWidth()
                    layoutParams.bottomMargin =
                        relativeLayout.measuredHeight - layoutParams.topMargin + view.scaledHeight()

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
    }

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

    private fun View.scaledWidth(): Int = (scaleFactor * this.width).toInt()
    private fun View.scaledHeight(): Int = (scaleFactor * this.height).toInt()
}