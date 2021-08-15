package bogomolov.aa.anochat.features.settings

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.main.Navigation
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.getFilePath
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun MiniatureView() {
    val viewModel = hiltViewModel<SettingsViewModel>(Navigation.navController!!.getBackStackEntry("settingsRoute"))
    val state = viewModel.state.collectAsState()
    Content(state.value, viewModel)
}

@Preview
@Composable
private fun Content(settingsState: SettingsUiState = testSettingsUiState, viewModel: SettingsViewModel? = null) {
    val state = settingsState.miniatureState!!
    val density = LocalDensity.current.density
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.set_avatar)) },
                navigationIcon = {
                    IconButton(onClick = { Navigation.navController?.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { createMiniature(context, viewModel!!) }) {
                Icon(
                    painterResource(id = R.drawable.ok_icon),
                    contentDescription = "",
                    modifier = Modifier.scale(1.5f)
                )
            }
        },
        content = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                val imageBitmap = state.miniature.bitmap?.asImageBitmap()
                if (imageBitmap != null) {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                if (state.imageWidth == 0)
                                    initDimensions(
                                        it.size.width,
                                        it.size.height,
                                        it.parentCoordinates?.size?.width ?: 0,
                                        viewModel!!
                                    )
                            }
                            .pointerInput(Unit) { detectMaskTransformGestures(false, viewModel!!) },
                        bitmap = imageBitmap,
                        contentScale = ContentScale.FillWidth,
                        contentDescription = ""
                    )
                }
                SelectorShape(state = state, density, viewModel!!)
            }
        }
    )
}

@Composable
private fun SelectorShape(state: MiniatureState, density: Float, viewModel: SettingsViewModel) {
    Box(contentAlignment = Alignment.TopStart, modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(3.dp, LightColorPalette.primary),
            color = Color(0x00FFFFFF),
            modifier = Modifier
                .size(
                    width = (state.maskImage.width * state.maskImage.scaleFactor / density).dp,
                    height = (state.maskImage.height * state.maskImage.scaleFactor / density).dp
                )
                .offset { IntOffset(state.maskImage.left, state.maskImage.top) }
                .pointerInput(Unit) { detectMaskTransformGestures(true, viewModel) }
        ) {}
    }
}

private suspend fun PointerInputScope.detectMaskTransformGestures(offset: Boolean, viewModel: SettingsViewModel) {
    detectTransformGestures(
        onGesture = { _, pan, gestureZoom, gestureRotate ->
            checkBounds(
                offset = if (offset) Pair(pan.x.toInt(), pan.y.toInt()) else null,
                scale = viewModel.currentState.miniatureState!!.maskImage.scaleFactor * gestureZoom,
                viewModel = viewModel
            )
        }
    )
}

private fun createMiniature(context: Context, viewModel: SettingsViewModel) {
    val state = viewModel.currentState.miniatureState!!
    val maskWidth = state.maskImage.width * state.maskImage.scaleFactor
    val maskHeight = state.maskImage.height * state.maskImage.scaleFactor
    val x = (state.maskX / state.initialImageScale).toInt()
    val y = (state.maskY / state.initialImageScale).toInt()
    val width = (maskWidth / state.initialImageScale).toInt()
    val height = (maskHeight / state.initialImageScale).toInt()

    val miniature = state.miniature
    val bitmap = miniature.bitmap!!
    val miniBitmap = Bitmap.createBitmap(
        bitmap,
        max(x, 0),
        max(y, 0),
        min(bitmap.width - Math.max(x, 0), width),
        min(bitmap.height - Math.max(y, 0), height)
    )
    val miniPhotoPath =
        getFilePath(context, getMiniPhotoFileName(miniature.name))
    miniBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(miniPhotoPath))

    viewModel.updateUser { copy(photo = viewModel.currentState.miniatureState?.miniature?.name) }
    Navigation.navController?.navigateUp()
}

private fun initDimensions(
    measuredWidth: Int,
    measuredHeight: Int,
    windowWidth: Int,
    viewModel: SettingsViewModel
) {
    val state = viewModel.currentState.miniatureState!!
    val maskImageWidth = state.maskImage.width
    val maskImageHeight = state.maskImage.height

    val bitmap = state.miniature.bitmap!!
    val koef1 = bitmap.width.toFloat() / bitmap.height.toFloat()
    val koef2 =
        measuredWidth.toFloat() / measuredHeight.toFloat()
    val imageWidth: Int
    val imageHeight: Int
    val initialImageScale: Float
    if (koef1 >= koef2) {
        imageWidth = measuredWidth
        imageHeight = (measuredWidth / koef1).toInt()
        initialImageScale = measuredWidth.toFloat() / bitmap.width.toFloat()
    } else {
        imageWidth = (koef1 * measuredHeight).toInt()
        imageHeight = measuredHeight
        initialImageScale = measuredHeight.toFloat() / bitmap.height.toFloat()
    }

    val maxScaleX = imageWidth.toFloat() / maskImageWidth
    val maxScaleY = imageHeight.toFloat() / maskImageHeight
    val maxScale = min(maxScaleX, maxScaleY)
    val scaleFactor = windowWidth.toFloat() / maskImageWidth
    viewModel.updateStateBlocking {
        copy(
            miniatureState = miniatureState?.copy(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                initialImageScale = initialImageScale,
                maxScale = maxScale,
            )
        )
    }
    checkBounds(scale = scaleFactor, viewModel = viewModel)
}

private fun checkBounds(
    offset: Pair<Int, Int>? = null,
    scale: Float? = null,
    viewModel: SettingsViewModel
) {
    val scaleFactor = scale ?: viewModel.currentState.miniatureState!!.maskImage.scaleFactor
    val state = viewModel.currentState.miniatureState!!

    var left = state.maskImage.left
    var top = state.maskImage.top
    if (offset != null) {
        left += offset.first
        top += offset.second
    }
    val maskImageWidth = state.maskImage.width * scaleFactor
    val maskImageHeight = state.maskImage.height * scaleFactor
    if (top < 0) top = 0
    if (left < 0) left = 0
    if (left + maskImageWidth > state.imageWidth)
        left = (state.imageWidth - maskImageWidth).toInt()
    if (top + maskImageHeight > state.imageHeight)
        top = (state.imageHeight - maskImageHeight).toInt()
    val maskX = left
    val maskY = top
    viewModel.updateState {
        copy(
            miniatureState = miniatureState?.copy(
                maskX = maskX,
                maskY = maskY,
                maskImage = miniatureState.maskImage.copy(
                    scaleFactor = max(0.5f, min(scaleFactor, state.maxScale)),
                    left = left,
                    top = top
                )
            )
        )
    }
}