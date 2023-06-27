package bogomolov.aa.anochat.features.shared

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.main.LocalNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_SCALE = 5f
private const val MIN_SCALE = 1f

@Composable
fun ImageView(imageName: String) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            bitmap = getBitmapFromGallery(imageName, context, 1)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var initialTop by remember { mutableStateOf(0) }
    var initialLeft by remember { mutableStateOf(0) }

    var scale by remember { mutableStateOf(1.0f) }
    var top by remember { mutableStateOf(initialTop) }
    var left by remember { mutableStateOf(0) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }
    var windowWidth by remember { mutableStateOf(0) }
    var windowHeight by remember { mutableStateOf(0) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxWidth()
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, gestureZoom, gestureRotate ->
                        scale *= gestureZoom
                        if (scale > MAX_SCALE) scale = MAX_SCALE
                        if (scale < MIN_SCALE) scale = MIN_SCALE

                        val scaleX = windowWidth.toFloat() / imageWidth
                        if (scale > scaleX) {
                            left += pan.x.toInt()
                            left = left.coerceAtLeast((initialLeft - (scale - scaleX) * imageWidth / 2).toInt())
                            left = left.coerceAtMost((initialLeft + (scale - scaleX) * imageWidth / 2).toInt())
                        }

                        val scaleY = windowHeight.toFloat() / imageHeight
                        if (scale > scaleY) {
                            top += pan.y.toInt()
                            top = top.coerceAtLeast((initialTop - (scale - scaleY) * imageHeight / 2).toInt())
                            top = top.coerceAtMost((initialTop + (scale - scaleY) * imageHeight / 2).toInt())
                        }
                    }
                )
            }
            .onGloballyPositioned {
                if (windowWidth == 0) {
                    windowWidth = it.size.width
                    windowHeight = it.size.height
                }
            }
    ) {
        val imageBitmap = bitmap?.asImageBitmap()
        if (imageBitmap != null) {
            val scaleWidth = screenHeight / screenWidth > imageBitmap.height / imageBitmap.width
            Image(
                modifier = Modifier
                    .onGloballyPositioned {
                        if (imageWidth == 0) {
                            imageWidth = it.size.width
                            imageHeight = it.size.height
                            initialTop = (windowHeight - imageHeight) / 2
                            initialLeft = (windowWidth - imageWidth) / 2
                            top += initialTop
                            left += initialLeft
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                            }
                        )
                    }
                    .offset { IntOffset(left, top) }
                    .scale(scale).run {
                        if (scaleWidth) fillMaxWidth() else fillMaxHeight()
                    },
                bitmap = imageBitmap,
                contentScale = if (scaleWidth) ContentScale.FillWidth else ContentScale.FillHeight,
                contentDescription = ""
            )
        }
        if (controlsVisible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BackButton(onClick = remember { { navController?.popBackStack() } })
                Icon(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = remember { { share(imageName, context) } }),
                    imageVector = Icons.Filled.Share,
                    tint = Color.White,
                    contentDescription = "Share"
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

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(
        modifier = Modifier
            .size(32.dp),
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            tint = Color.White,
            contentDescription = "Back"
        )
    }
}