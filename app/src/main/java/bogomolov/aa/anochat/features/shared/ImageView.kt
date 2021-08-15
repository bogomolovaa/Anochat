package bogomolov.aa.anochat.features.shared

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.main.Navigation

private const val MAX_SCALE = 5f
private const val MIN_SCALE = 1f
private const val TAG = "ImageView"

@Composable
fun ImageView(imageName: String, fromGallery: Boolean) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(0) {
        bitmap = loadImage(
            imageName = imageName,
            quality = 1,
            fromGallery = fromGallery,
            context = context
        )
    }

    val scale = remember { mutableStateOf(1.0f) }
    val top = remember { mutableStateOf(0) }
    val left = remember { mutableStateOf(0) }
    val imageWidth = remember { mutableStateOf(0) }
    val imageHeight = remember { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .background(Color.Black)
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
        val imageBitmap = bitmap?.asImageBitmap()
        if (imageBitmap != null)
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
                                //expand(!expanded)
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
                        Navigation.navController?.popBackStack()
                    },
                imageVector = Icons.Filled.ArrowBack,
                tint = Color.White,
                contentDescription = "Back"
            )
            Icon(
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        share(imageName, context)
                    },
                imageVector = Icons.Filled.Share,
                tint = Color.White,
                contentDescription = "Back"
            )
        }
    }
}

private fun share(imageName: String, context: Context) {
    val uriWithSource = getUriWithSource(imageName, context)
    if (uriWithSource.uri != null) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_STREAM, uriWithSource.uri)
        if (!uriWithSource.fromGallery) intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val title = context.resources.getString(R.string.share_image)
        context.startActivity(Intent.createChooser(intent, title))
    }
}

private fun loadImage(imageName: String, quality: Int, fromGallery: Boolean, context: Context): Bitmap? {
    try {
        return if (fromGallery) getBitmapFromGallery(imageName, context, quality)
        else getBitmap(imageName, context, quality)
    } catch (e: Exception) {
        Log.w(TAG, "image not loaded", e)
    }
    return null
}

/*
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

 */