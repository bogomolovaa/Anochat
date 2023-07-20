package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.AttachmentStatus
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.main.theme.REPORT_MESSAGE_COLOR0
import bogomolov.aa.anochat.features.main.theme.REPORT_MESSAGE_COLOR1
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


@Composable
fun ReplyMessage(
    modifier: Modifier = Modifier,
    message: Message,
    bitmap: MutableState<ImageBitmap?> = mutableStateOf(null),
    replyPlayingState: PlayingState?,
    playOnClick: (audioFile: String?, messageId: String?) -> Unit = { _, _ -> },
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(), color = MaterialTheme.colorScheme.secondary
            ) { }
            if (message.audio != null) {
                PlayAudio(replyPlayingState, message.audio, message.messageId, playOnClick)
            } else {
                if (message.text.isNotEmpty())
                    Text(
                        text = message.text,
                        style = TextStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                        modifier = Modifier
                            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                            .widthIn(max = 300.dp),
                        maxLines = 2
                    )
                bitmap.value?.let {
                    Image(
                        modifier = Modifier.heightIn(max = 48.dp),
                        bitmap = it,
                        contentDescription = null
                    )
                }
            }
        }
        if (onClear != null)
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClear() }
            )
    }

}

@Composable
fun PlayAudio(
    state: PlayingState? = testPlayingState,
    audio: String? = null,
    messageId: String? = null,
    playOnClick: (audioFile: String?, messageId: String?) -> Unit = { _, _ -> },
    onClear: (() -> Unit)? = null
) {
    Row(
        Modifier.height(56.dp)
    ) {
        Icon(imageVector = if (audio == null) Icons.Filled.ErrorOutline else if (state?.paused != false) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = null,
            tint = if (audio == null) Color.Red else LocalContentColor.current,
            modifier = Modifier
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
                .size(36.dp)
                .clickable {
                    playOnClick(audio, messageId)
                })
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(142.dp)
                .padding(start = 8.dp, end = 8.dp)
        ) {
            LinearProgressIndicator(modifier = Modifier.align(Alignment.Center), progress = state?.let {
                if (state.duration > 0) state.elapsed.toFloat() / state.duration else 0f
            } ?: 0f)
            Text(
                text = timeToString(state?.elapsed),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 2.dp)
            )
            Text(
                text = timeToString(state?.duration),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 12.dp, end = 4.dp)
                .size(36.dp)
        )
        if (onClear != null)
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClear() }
            )
    }
}

@Composable
fun DateDelimiterCompose(delimiter: DateDelimiter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp)
                .align(CenterHorizontally)
                .shadow(shape = MaterialTheme.shapes.small, spotColor = Color.Black, elevation = 2.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = delimiter.time,
                modifier = Modifier.padding(6.dp),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(widthDp = 320)
@ExperimentalMaterial3Api
@Composable
fun MessageCompose(
    message: Message = testMessage,
    playingState: PlayingState? = testPlayingState,
    replyPlayingState: PlayingState? = null,
    selected: Boolean = false,
    loadingBitmaps: Boolean = false,
    bitmap: MutableState<ImageBitmap?> = mutableStateOf(null),
    replyBitmap: MutableState<ImageBitmap?> = mutableStateOf(null),
    onClick: () -> Unit = {},
    onSelect: () -> Unit = {},
    onSwipe: () -> Unit = {},
    playOnClick: (audioFile: String?, messageId: String?) -> Unit = { _, _ -> }
) {
    var windowWidth by remember { mutableStateOf(0) }
    Column(modifier = Modifier
        .onGloballyPositioned {
            windowWidth = it.size.width
        }
        .fillMaxWidth()
    ) {
        val offsetX = remember { Animatable(0f) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) MaterialTheme.colorScheme.tertiaryContainer else colorResource(android.R.color.transparent))
                .padding(4.dp)
        ) {
            val coroutineScope = rememberCoroutineScope()
            Card(modifier = Modifier
                .widthIn(min = 120.dp, max = 258.dp)
                .width(IntrinsicSize.Max)
                .align(if (message.isMine) Alignment.End else Alignment.Start)
                .pointerInput(message.messageId) {
                    detectTapGestures(onLongPress = {
                        onSelect()
                    })
                }
                .pointerInput(message.messageId) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        if (dragAmount > 40) {
                            onSwipe()
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = windowWidth.toFloat(),
                                    initialVelocity = 0f,
                                    animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                                )
                                offsetX.snapTo(0f)
                            }
                        }
                    }
                }
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .shadow(shape = MaterialTheme.shapes.small, spotColor = Color.Black, elevation = 2.dp),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isMine) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    message.replyMessage?.let {
                        ReplyMessage(Modifier.fillMaxWidth(), it, replyBitmap, replyPlayingState, playOnClick)
                    }
                    if (message.audio != null) {
                        PlayAudio(playingState, message.audio, message.messageId, playOnClick)
                    } else if (message.image != null || message.video != null) {
                        Row {
                            Box(modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        onClick()
                                    },
                                    onLongClick = {
                                        onSelect()
                                    }
                                )
                                .padding(bottom = 4.dp)) {
                                when {
                                    loadingBitmaps -> {
                                        Box(
                                            Modifier
                                                .size(250.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {

                                        }
                                    }

                                    message.attachmentStatus == AttachmentStatus.LOADING -> {
                                        Box(
                                            Modifier
                                                .size(250.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .align(Alignment.Center),
                                            )
                                        }
                                    }

                                    message.attachmentStatus == AttachmentStatus.LOADED && bitmap.value != null -> {
                                        bitmap.value?.let {
                                            Image(
                                                modifier = Modifier.size(250.dp),
                                                contentScale = ContentScale.Crop,
                                                bitmap = it,
                                                contentDescription = null
                                            )
                                        }
                                        if (message.video != null) Image(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .align(Alignment.Center),
                                            painter = painterResource(R.drawable.ic_play_circle),
                                            contentDescription = null
                                        )
                                    }

                                    message.attachmentStatus == AttachmentStatus.NOT_LOADED || (message.attachmentStatus == AttachmentStatus.LOADED && bitmap == null) -> {
                                        Box(
                                            Modifier
                                                .size(250.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ErrorOutline,
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .align(Alignment.Center),
                                                tint = Color.Red,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (message.text.isNotEmpty()) Row {
                        val annotatedString = textToAnnotatedString(message.text)
                        val context = LocalContext.current
                        val toolbarColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
                        val navbarColor = MaterialTheme.colorScheme.background.toArgb()
                        Text(
                            modifier = Modifier
                                .padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 1.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        annotatedString
                                            .getStringAnnotations("URL", 0, annotatedString.length)
                                            .firstOrNull()
                                            ?.let { openUrl(context, it.item, toolbarColor, navbarColor) }
                                    },
                                    onLongClick = {
                                        onSelect()
                                    }
                                ),
                            text = annotatedString,
                            style = TextStyle(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(4.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(end = 4.dp),
                        text = message.shortTimeString(),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                    if (message.isMine) {
                        if (!message.sent())
                            Icon(
                                imageVector = Icons.Filled.WatchLater,
                                modifier = Modifier.height(16.dp),
                                tint = REPORT_MESSAGE_COLOR0,
                                contentDescription = null
                            )
                        if (message.sentAndNotReceived() || message.receivedAndNotViewed())
                            Icon(
                                imageVector = Icons.Filled.Done,
                                modifier = Modifier.height(16.dp),
                                tint = if (message.sentAndNotReceived()) REPORT_MESSAGE_COLOR0
                                else REPORT_MESSAGE_COLOR1,
                                contentDescription = null
                            )
                        if (message.viewed())
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                modifier = Modifier.height(16.dp),
                                tint = REPORT_MESSAGE_COLOR1,
                                contentDescription = null
                            )
                        if (message.error())
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                modifier = Modifier.height(16.dp),
                                tint = Color.Red,
                                contentDescription = null
                            )
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String, toolbarColor: Int, navbarColor: Int) {
    val intent = CustomTabsIntent
        .Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams
                .Builder()
                .setToolbarColor(toolbarColor)
                .setNavigationBarColor(navbarColor)
                .build()
        )
        .build()
    intent.launchUrl(context, Uri.parse(url))
}


@SuppressLint("SimpleDateFormat")
private fun timeToString(time: Long?) = time?.let { SimpleDateFormat("mm:ss").format(Date(time)) } ?: "0:00"

@Composable
private fun textToAnnotatedString(text: String) = with(AnnotatedString.Builder()) {
    append(text)
    "(https|http)://[^ ]+".toRegex().findAll(text).forEach {
        val start = it.range.first
        val end = it.range.last + 1
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.secondary, textDecoration = TextDecoration.Underline
            ), start = start, end = end
        )
        addStringAnnotation(
            tag = "URL", annotation = it.value, start = start, end = end
        )
    }
    toAnnotatedString()
}

data class DateDelimiter(
    val time: String
)

val testPlayingState = PlayingState(
    audioFile = "", duration = 60 * 1000, elapsed = 20 * 1000, paused = false
)

val testMessage = Message(
    id = 0L,
    text = "some very",
    //text = "",
    time = System.currentTimeMillis(),
    isMine = false,
    image = null,
    //audio = "xxx",
    video = null,
    sent = 1,
    received = 1,
    viewed = 1,
    replyMessage = Message(text = "reply")
)

//playingState = testPlayingState
//bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.RGB_565)
//replyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)