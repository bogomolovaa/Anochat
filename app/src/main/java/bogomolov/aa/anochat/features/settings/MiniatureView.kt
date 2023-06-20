package bogomolov.aa.anochat.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.shared.*

@Composable
fun MiniatureView() {
    val navController = LocalNavController.current
    val backStackEntry = remember { navController!!.getBackStackEntry("settingsRoute") }
    val viewModel = hiltViewModel<SettingsViewModel>(backStackEntry)
    viewModel.events.collectEvents {
        if (it is MiniatureCreatedEvent) navController?.popBackStack()
    }
    viewModel.state.collectState { Content(it, viewModel) }
}

@Preview
@Composable
private fun Content(
    settingsState: SettingsUiState = testSettingsUiState,
    viewModel: SettingsViewModel? = null
) {
    val state = settingsState.miniatureState!!
    val density = LocalDensity.current.density
    val context = LocalContext.current
    val navController = LocalNavController.current
    val initDimensions: (LayoutCoordinates) -> Unit = remember {
        {
            viewModel?.initDimensions(
                measuredWidth = it.size.width,
                measuredHeight = it.size.height,
                windowWidth = it.parentCoordinates?.size?.width ?: 0
            )
        }
    }
    val checkBounds: (offset: Boolean, x: Int, y: Int, zoom: Float) -> Unit = remember {
        { offset: Boolean, x: Int, y: Int, zoom: Float ->
            viewModel?.checkBounds(offset, x, y, zoom)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.set_avatar)) },
                navigationIcon = {
                    IconButton(onClick = remember { { navController?.popBackStack() } }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = remember {
                { viewModel?.createMiniature(getFilePath(context, getMiniPhotoFileName(state.miniature.name))) }
            }) {
                Icon(
                    painterResource(id = R.drawable.ok_icon),
                    contentDescription = "",
                    modifier = Modifier.scale(1.5f)
                )
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth()
            ) {
                state.miniature.bitmap?.asImageBitmap()?.let {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                initDimensions(it)
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures(
                                    onGesture = { _, pan, gestureZoom, _ ->
                                        checkBounds(false, pan.x.toInt(), pan.y.toInt(), gestureZoom)
                                    }
                                )
                            },
                        bitmap = it,
                        contentScale = ContentScale.FillWidth,
                        contentDescription = ""
                    )
                }
                SelectorShape(state = state, density, checkBounds)
            }
        }
    )
}

@Composable
private fun SelectorShape(
    state: MiniatureState,
    density: Float,
    checkBounds: (offset: Boolean, x: Int, y: Int, zoom: Float) -> Unit
) {
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
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { _, pan, gestureZoom, _ ->
                            checkBounds(true, pan.x.toInt(), pan.y.toInt(), gestureZoom)
                        }
                    )
                }
        ) {}
    }
}