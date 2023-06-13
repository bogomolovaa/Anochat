package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.AttachmentStatus
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.LightColorPalette
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ReplyMessage(
    message: Message,
    bitmap: Bitmap?,
    replyPlayingState: PlayingState?,
    playOnClick: (audioFile: String?, messageId: String?) -> Unit = { _, _ -> },
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .background(color = colorResource(id = R.color.time_message_color))
            .height(48.dp)
    ) {
        Surface(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight(),
            color = colorResource(R.color.colorAccent)
        ) { }
        if (message.audio != null) {
            PlayAudio(replyPlayingState, message.audio, message.messageId, playOnClick)
        } else {
            if (message.text.isNotEmpty())
                Text(
                    text = message.text,
                    style = TextStyle(fontSize = 14.sp),
                    modifier = Modifier
                        .padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                        .widthIn(max = 300.dp),
                    maxLines = 2
                )
            if (bitmap != null)
                Image(
                    modifier = Modifier.heightIn(max = 48.dp),
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null
                )
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
        Modifier
            .height(56.dp)
    ) {
        Icon(
            imageVector = if (audio == null) Icons.Filled.ErrorOutline else
                if (state?.paused != false) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = null,
            tint = if (audio == null) Color.Red else Color.Black,
            modifier = Modifier
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
                .size(36.dp)
                .clickable {
                    playOnClick(audio, messageId)
                }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(142.dp)
                .padding(start = 8.dp, end = 8.dp)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                progress = state?.let {
                    if (state.duration > 0) state.elapsed.toFloat() / state.duration else 0f
                } ?: 0f
            )
            Text(
                text = timeToString(state?.elapsed), modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 2.dp)
            )
            Text(
                text = timeToString(state?.duration), modifier = Modifier
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

@Preview(widthDp = 320)
@ExperimentalMaterialApi
@Composable
fun MessageCompose(
    data: MessageViewData? = testMessageViewData,
    bitmap: Bitmap? = null,
    replyBitmap: Bitmap? = null,
    onClick: () -> Unit = {},
    onSwipe: () -> Unit = {},
    playOnClick: (audioFile: String?, messageId: String?) -> Unit = { _, _ -> }
) {
    if (data == null) return
    var windowWidth by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                windowWidth = it.size.width
            }
            .padding(bottom = 4.dp)
    ) {
        if (data.hasTimeMessage()) {
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .align(CenterHorizontally),
                shape = RoundedCornerShape(6.dp),
                elevation = 1.dp,
                backgroundColor = colorResource(id = R.color.time_message_color)
            ) {
                Text(
                    text = data.dateDelimiter!!,
                    modifier = Modifier.padding(6.dp),
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
            }
        }
        val message = data.message
        val offsetX = remember { Animatable(0f) }
        Card(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 258.dp)
                .align(if (message.isMine) Alignment.End else Alignment.Start)
                .pointerInput(data.message.messageId) {
                    coroutineScope {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 40) {
                                onSwipe()
                                launch {
                                    offsetX.animateTo(
                                        targetValue = windowWidth.toFloat(),
                                        initialVelocity = 0f,
                                        animationSpec = tween(
                                            durationMillis = 500,
                                            easing = LinearEasing
                                        )
                                    )
                                    offsetX.snapTo(0f)
                                }
                            }
                        }
                    }
                }
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            shape = RoundedCornerShape(6.dp),
            elevation = 1.dp,
            backgroundColor = colorResource(
                id =
                if (message.isMine) R.color.my_message_color
                else R.color.not_my_message_color
            )
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                message.replyMessage?.let {
                    ReplyMessage(it, replyBitmap, data.replyPlayingState, playOnClick)
                }
                if (message.audio != null) {
                    PlayAudio(data.playingState, message.audio, message.messageId, playOnClick)
                } else if (message.image != null || message.video != null) {
                    Row {
                        Box(modifier = Modifier
                            .clickable { onClick() }
                            .padding(bottom = 4.dp)) {
                            when {
                                message.attachmentStatus == AttachmentStatus.LOADING -> {
                                    Box(
                                        Modifier
                                            .size(250.dp)
                                            .background(colorResource(id = R.color.time_message_color))
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .align(Alignment.Center),
                                        )
                                    }
                                }
                                message.attachmentStatus == AttachmentStatus.LOADED && bitmap != null -> {
                                    Image(
                                        modifier = Modifier.size(250.dp),
                                        contentScale = ContentScale.Crop,
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null
                                    )
                                    if (message.video != null)
                                        Image(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .align(Alignment.Center),
                                            painter = painterResource(R.drawable.ic_play_circle),
                                            contentDescription = null
                                        )
                                }
                                message.attachmentStatus == AttachmentStatus.NOT_LOADED ||
                                        (message.attachmentStatus == AttachmentStatus.LOADED && bitmap == null) -> {
                                    Box(
                                        Modifier
                                            .size(250.dp)
                                            .background(colorResource(id = R.color.time_message_color))
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
                if (message.text.isNotEmpty())
                    Row {
                        val annotatedString = textToAnnotatedString(message.text)
                        val uriHandler = LocalUriHandler.current
                        ClickableText(
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 1.dp),
                            text = annotatedString,
                            style = TextStyle(fontSize = 16.sp),
                            onClick = {
                                annotatedString
                                    .getStringAnnotations("URL", it, it)
                                    .firstOrNull()?.let { stringAnnotation ->
                                        uriHandler.openUri(stringAnnotation.item)
                                    }
                            }
                        )
                    }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text(
                        modifier = Modifier.padding(end = 4.dp),
                        text = message.shortTimeString(),
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                    if (message.isMine) {
                        if (!data.sent())
                            Icon(
                                imageVector = Icons.Filled.WatchLater,
                                modifier = Modifier.height(16.dp),
                                tint = colorResource(id = R.color.report_message_color0),
                                contentDescription = null
                            )
                        if (data.sentAndNotReceived() || data.receivedAndNotViewed())
                            Icon(
                                imageVector = Icons.Filled.Done,
                                modifier = Modifier.height(16.dp),
                                tint = colorResource(
                                    id = if (data.sentAndNotReceived()) R.color.report_message_color0
                                    else R.color.report_message_color1
                                ),
                                contentDescription = null
                            )
                        if (data.viewed())
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                modifier = Modifier.height(16.dp),
                                tint = colorResource(id = R.color.report_message_color1),
                                contentDescription = null
                            )
                        if (data.error())
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


@SuppressLint("SimpleDateFormat")
private fun timeToString(time: Long?) = time?.let { SimpleDateFormat("mm:ss").format(Date(time)) } ?: "0:00"

private fun textToAnnotatedString(text: String) = with(AnnotatedString.Builder()) {
    append(text)
    "(https|http)://[^ ]+".toRegex().findAll(text).forEach {
        val start = it.range.first
        val end = it.range.last + 1
        addStyle(
            style = SpanStyle(
                color = LightColorPalette.primary,
                textDecoration = TextDecoration.Underline
            ),
            start = start,
            end = end
        )
        addStringAnnotation(
            tag = "URL",
            annotation = it.value,
            start = start,
            end = end
        )
    }
    toAnnotatedString()
}

val testPlayingState = PlayingState(
    audioFile = "",
    duration = 60 * 1000,
    elapsed = 20 * 1000,
    paused = false
)

private val testMessageViewData = MessageViewData(
    Message(
        id = 0L,
        //text = "some very very long text",
        text = "",
        time = System.currentTimeMillis(),
        isMine = false,
        image = null,
        audio = "xxx",
        video = null,
        sent = 1,
        received = 1,
        viewed = 1,
        //replyMessage = Message(text = "reply to message")
    )
).apply {
    playingState = testPlayingState
    //bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.RGB_565)
    //replyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
    dateDelimiter = "16 july"
}