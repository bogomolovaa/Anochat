package bogomolov.aa.anochat.view.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.databinding.FragmentMiniatureBinding
import java.io.File

class MiniatureFragment : Fragment(), View.OnTouchListener {
    private lateinit var binding: FragmentMiniatureBinding
    private var finalHeight = 0f
    private var finalWidth = 0f
    private lateinit var bitmap: Bitmap

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


        binding.maskImage.setOnTouchListener(this)

        relativeLayout = binding.layout

        return binding.root
    }


    var lastPoint: Point = Point()
    var relativeLayout: RelativeLayout? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        //1. user's finger
        val point = Point(event.rawX.toInt(), event.rawY.toInt())

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                // 2. record the last touch point
                lastPoint = point
            }
            MotionEvent.ACTION_MOVE -> {
                // 3. get the move offset
                val offset = Point(point.x - lastPoint.x, point.y - lastPoint.y)
                val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
                layoutParams.leftMargin += offset.x
                layoutParams.topMargin += offset.y
                // * also check right/bottom Margin
                layoutParams.rightMargin =
                    relativeLayout!!.measuredWidth - layoutParams.leftMargin + view.measuredWidth
                layoutParams.bottomMargin =
                    relativeLayout!!.measuredHeight - layoutParams.topMargin + view.measuredHeight

                if (layoutParams.topMargin < 0) layoutParams.topMargin = 0
                if (layoutParams.leftMargin < 0) layoutParams.leftMargin = 0

                var imageWidth = 0
                var imageHeight = 0
                val koef1 = bitmap.width.toFloat() / bitmap.height.toFloat()
                val koef2 = binding.imageView.measuredWidth.toFloat() / binding.imageView.measuredHeight.toFloat()
                if (koef1 >= koef2) {
                    imageWidth = binding.imageView.measuredWidth
                    imageHeight = (binding.imageView.measuredWidth / koef1).toInt()
                } else {
                    imageWidth = (koef1 * binding.imageView.measuredHeight).toInt()
                    imageHeight = binding.imageView.measuredHeight
                }

                if (layoutParams.leftMargin + view.measuredWidth > imageWidth)
                    layoutParams.leftMargin = imageWidth - view.measuredWidth
                if (layoutParams.topMargin + view.measuredHeight > imageHeight)
                    layoutParams.topMargin = imageHeight - view.measuredHeight

                view.layoutParams = layoutParams
                // 4. record the last touch point
                lastPoint = point;
            }
        }
        return true
    }


}
