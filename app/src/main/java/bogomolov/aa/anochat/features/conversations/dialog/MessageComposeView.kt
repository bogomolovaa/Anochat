package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.animatedVectorResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.AttachmentStatus
import bogomolov.aa.anochat.domain.entity.Message
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun ReplyMessage(message: Message, bitmap: Bitmap?, replyPlayingState: PlayingState?) {
    Row(modifier = Modifier.background(color = colorResource(id = R.color.time_message_color))) {
        Row(
            modifier = Modifier
                .size(4.dp, 48.dp)
                .background(colorResource(R.color.colorAccent))
        ) { }
        if (message.audio != null) {
            PlayAudio(replyPlayingState)
        } else {
            Text(
                text = message.text,
                modifier = Modifier
                    .padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                    .height(32.dp)
            )
            if (bitmap != null)
                Image(
                    modifier = Modifier.heightIn(max = 48.dp),
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null
                )
        }
    }

}

@Composable
fun PlayAudio(state: PlayingState? = testPlayingState) {
    Row(
        Modifier
            .height(60.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                .size(36.dp)
        )
        if (state != null)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(142.dp)
                    .padding(start = 8.dp, end = 8.dp)
            ) {
                if (state.duration > 0)
                    LinearProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        progress = state.elapsed.toFloat() / state.duration
                    )
                Text(
                    text = timeToString(state.elapsed), modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 2.dp)
                )
                Text(
                    text = timeToString(state.duration), modifier = Modifier
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
        Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier
                .size(20.dp)
        )
    }
}

@ExperimentalMaterialApi
@Preview(widthDp = 320)
@Composable
fun MessageCompose(
    data: MessageViewData = testMessageViewData,
    onClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (data.hasTimeMessage()) {
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .align(CenterHorizontally),
                shape = RoundedCornerShape(6.dp),
                elevation = 1.dp,
                backgroundColor = colorResource(id = R.color.time_message_color)
            ) {
                Text(text = data.dateDelimiter!!, modifier = Modifier.padding(6.dp))
            }
        }
        val message = data.message
        Card(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 258.dp)
                .align(if (message.isMine) Alignment.End else Alignment.Start),
            shape = RoundedCornerShape(6.dp),
            elevation = 1.dp,
            backgroundColor = colorResource(
                id =
                if (message.isMine) R.color.my_message_color
                else R.color.not_my_message_color
            ),
            onClick = onClick
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                message.replyMessage?.let {
                    Row(modifier = Modifier.height(48.dp)) {
                        ReplyMessage(it, data.replyBitmap, data.replyPlayingState)
                    }
                }
                if (message.audio != null) PlayAudio(data.playingState)
                Row {
                    if (message.hasAttachment()) {
                        val bitmap = data.bitmap
                        Box {
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
                Row {
                    Text(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 1.dp),
                        text = message.text,
                        fontSize = 16.sp
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
private fun timeToString(time: Long) = SimpleDateFormat("mm:ss").format(Date(time))

private val testPlayingState = PlayingState(
    audioFile = "",
    duration = 60 * 1000,
    elapsed = 20 * 1000,
    paused = false
)

private val testMessageViewData = MessageViewData(
    Message(
        id = 0L,
        //text = "some very very long text",
        text = "short",
        time = System.currentTimeMillis(),
        isMine = false,
        image = null,
        video = null,
        sent = 1,
        received = 1,
        viewed = 1,
        //replyMessage = Message(text = "reply to message")
    )
).apply {
    //playingState = testPlayingState
    bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.RGB_565)
    replyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
    dateDelimiter = "16 july"
}

