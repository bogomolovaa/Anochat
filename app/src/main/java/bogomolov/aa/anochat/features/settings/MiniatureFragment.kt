package bogomolov.aa.anochat.features.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentMiniatureBinding
import bogomolov.aa.anochat.features.shared.getFilePath
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_miniature.*
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class MiniatureFragment : Fragment() {
    private val viewModel: SettingsViewModel by hiltNavGraphViewModels(R.id.settings_graph)
    private lateinit var binding: FragmentMiniatureBinding
    private lateinit var bitmap: Bitmap
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentMiniatureBinding.inflate(inflater, container, false).also { binding = it }.root

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        bitmap = viewModel.miniature.bitmap!!
        binding.imageView.setImageBitmap(bitmap)
        binding.fab.setOnClickListener {
            createMiniature()
            viewModel.addAction(UpdateUserAction { copy(photo = viewModel.miniature.name) })
            navController.navigateUp()
        }
        scaleDetector = ScaleGestureDetector(context, scaleListener)
        binding.imageView.setOnTouchListener(maskImageOnTouchListener)
        binding.imageView.post {
            setImageRealDimensions()
            val windowSize = getWindowSize()
            scaleFactor = windowSize.first.toFloat() / binding.maskImage.width.toFloat()
            binding.maskImage.scaleX = scaleFactor
            binding.maskImage.scaleY = scaleFactor
            checkBounds(binding.maskImage.layoutParams as ConstraintLayout.LayoutParams)
        }
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
        val miniPhotoPath =
            getFilePath(requireContext(), getMiniPhotoFileName(viewModel.miniature.name))
        miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))
    }

    private var initialImageScale = 1f
    private var maskX = 0
    private var maskY = 0
    private var scaleFactor = 1f
    private var scaling = false

    private var imageWidth = 0
    private var imageHeight = 0
    private var maxScale = 1f

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaling = true
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            scaling = false
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.5f, min(scaleFactor, maxScale))
            binding.maskImage.scaleX = scaleFactor
            binding.maskImage.scaleY = scaleFactor
            checkBounds(binding.maskImage.layoutParams as ConstraintLayout.LayoutParams)
            return true
        }
    }

    private fun getWindowSize(): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    private val maskImageOnTouchListener = object : View.OnTouchListener {
        private var lastPoint: Point = Point()
        private var canMove = true

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            if (!scaling) {
                val maskImage = binding.maskImage
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
                            val layoutParams =
                                maskImage.layoutParams as ConstraintLayout.LayoutParams
                            layoutParams.leftMargin += offset.x
                            layoutParams.topMargin += offset.y
                            layoutParams.rightMargin =
                                imageView.measuredWidth - layoutParams.leftMargin + maskImage.scaledWidth()
                            layoutParams.bottomMargin =
                                imageView.measuredHeight - layoutParams.topMargin + maskImage.scaledHeight()

                            checkBounds(layoutParams)
                        }
                        lastPoint = point
                    }
                    MotionEvent.ACTION_UP -> canMove = false
                }
            }
            return false
        }
    }

    private fun checkBounds(layoutParams: ConstraintLayout.LayoutParams) {
        val dx = (maskImage.scaledWidth() - maskImage.width) / 2
        val dy = (maskImage.scaledHeight() - maskImage.height) / 2
        if (layoutParams.topMargin < dy) layoutParams.topMargin = dy
        if (layoutParams.leftMargin < dx) layoutParams.leftMargin = dx
        if (layoutParams.leftMargin + maskImage.width + dx > imageWidth)
            layoutParams.leftMargin = imageWidth - maskImage.width - dx
        if (layoutParams.topMargin + maskImage.height + dy > imageHeight)
            layoutParams.topMargin = imageHeight - maskImage.height - dy
        maskX = layoutParams.leftMargin - dx
        maskY = layoutParams.topMargin - dy
        maskImage.layoutParams = layoutParams
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
        maxScale = min(maxScaleX, maxScaleY)
    }

    private fun View.scaledWidth(): Int = (scaleFactor * this.width).toInt()
    private fun View.scaledHeight(): Int = (scaleFactor * this.height).toInt()
}