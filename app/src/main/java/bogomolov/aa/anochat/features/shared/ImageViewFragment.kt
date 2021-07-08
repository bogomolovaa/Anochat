package bogomolov.aa.anochat.features.shared

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bogomolov.aa.anochat.R
import kotlin.math.exp


private const val MAX_SCALE = 5f
private const val MIN_SCALE = 1f
private const val TAG = "ImageViewFragment"

class ImageViewFragment : Fragment() {
    private var bitmap: Bitmap? = null
    private var savedSystemUiVisibility = 0
    private var expanded = false
    private lateinit var imageName: String
    private var fromGallery = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            imageName = arguments?.getString("image")!!
            val quality = arguments?.getInt("quality")!!
            fromGallery = arguments?.getBoolean("gallery") ?: false
            loadImage(quality)

            setContent {
                Content(bitmap!!)
            }
        }

    @Composable
    private fun Content(bitmap: Bitmap) {
        val scale = remember { mutableStateOf(1.0f) }
        val top = remember { mutableStateOf(0) }
        val left = remember { mutableStateOf(0) }
        val imageWidth = remember { mutableStateOf(0) }
        val imageHeight = remember { mutableStateOf(0) }
        MaterialTheme(
            colors = LightColorPalette
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTransformGestures(
                            onGesture = { _, pan, gestureZoom, gestureRotate ->
                                scale.value *= gestureZoom
                                if (scale.value > MAX_SCALE) scale.value = MAX_SCALE
                                if (scale.value < MIN_SCALE) scale.value = MIN_SCALE
                                left.value += pan.x.toInt()
                                top.value += pan.y.toInt()
                                if (left.value < -(scale.value - 1) * imageWidth.value / 2)
                                    left.value = -((scale.value - 1) * imageWidth.value / 2).toInt()
                                if (left.value > (scale.value - 1) * imageWidth.value / 2)
                                    left.value = ((scale.value - 1) * imageWidth.value / 2).toInt()

                                if (top.value < -(scale.value - 1) * imageHeight.value / 2)
                                    top.value = -((scale.value - 1) * imageHeight.value / 2).toInt()
                                if (top.value > (scale.value - 1) * imageHeight.value / 2)
                                    top.value = ((scale.value - 1) * imageHeight.value / 2).toInt()
                            }
                        )
                    }
            ) {
                val imageBitmap = bitmap.asImageBitmap()
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            if (imageWidth.value == 0) {
                                imageWidth.value = it.size.width
                                imageHeight.value = it.size.height
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    expand(!expanded)
                                }
                            )
                        }
                        .offset { IntOffset(left.value, top.value).also { println("IntOffset $it") } }
                        .scale(scale.value),
                    bitmap = imageBitmap,
                    contentScale = ContentScale.FillWidth,
                    contentDescription = ""
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        modifier = Modifier
                            .clickable {
                                findNavController().popBackStack()
                            },
                        imageVector = Icons.Filled.ArrowBack,
                        tint = Color.White,
                        contentDescription = "Back"
                    )
                    Icon(
                        modifier = Modifier.size(32.dp)
                            .clickable {
                                share()
                            },
                        imageVector = Icons.Filled.Share,
                        tint = Color.White,
                        contentDescription = "Back"
                    )
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedSystemUiVisibility = requireActivity().window.decorView.systemUiVisibility

        requireActivity().window.decorView.setBackgroundResource(R.color.black)
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.black)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.decorView.systemUiVisibility = savedSystemUiVisibility
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


    private fun share() {
        val uriWithSource = getUriWithSource(imageName, requireContext())
        if (uriWithSource.uri != null) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_STREAM, uriWithSource.uri)
            if (!uriWithSource.fromGallery) intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val title = resources.getString(R.string.share_image)
            startActivity(Intent.createChooser(intent, title))
        }
    }

    private fun loadImage(quality: Int) {
        try {
            bitmap = if (fromGallery) getBitmapFromGallery(imageName, requireContext(), quality)
            else getBitmap(imageName, requireContext(), quality)
        } catch (e: Exception) {
            Log.w(TAG, "image not loaded", e)
        }
    }
}