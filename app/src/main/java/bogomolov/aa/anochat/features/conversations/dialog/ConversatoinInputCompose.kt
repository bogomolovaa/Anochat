package bogomolov.aa.anochat.features.conversations.dialog

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.conversations.list.testConversation
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun ConversationInput(
    inputState: InputState = testDialogUiState.inputState,
    playingState: PlayingState? = testPlayingState,
    playOnClick: (audioFile: String?, messageId: String?) -> Unit = { _, _ -> },
    onTextChanged: (String) -> Unit = {},
    onClear: () -> Unit = {},
    emojiOnClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(end = 64.dp)
            .fillMaxWidth()
            .heightIn(min = 60.dp)
    ) {
        when (inputState.state) {
            InputState.State.INITIAL, InputState.State.TEXT_ENTERED, InputState.State.FAB_EXPAND -> {
                Surface(
                    modifier = Modifier.padding(start = 4.dp),
                    shape = RoundedCornerShape(25.dp),
                    color = Color.White
                ) {
                    /*
                    Icon(
                        modifier = Modifier
                            .padding(start = 8.dp, top = 15.dp)
                            .clickable {
                                emojiOnClick()
                            },
                        imageVector = Icons.Outlined.EmojiEmotions,
                        contentDescription = null
                    )
                     */
                    TextField(
                        value = inputState.text,
                        onValueChange = { onTextChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        maxLines = 5,
                        placeholder = {
                            Text(stringResource(id = R.string.enter_message))
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            disabledTextColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            InputState.State.VOICE_RECORDING -> {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .size(36.dp)
                        .align(CenterVertically)
                )
                Text(text = stringResource(id = R.string.audio_message), modifier = Modifier.align(CenterVertically))
                Text(text = " " + inputState.audioLengthText, modifier = Modifier.align(CenterVertically))
                Icon(
                    imageVector = Icons.Filled.Circle,
                    tint = Color.Red,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 4.dp)
                        .size(24.dp)
                        .align(CenterVertically)
                )
            }
            InputState.State.VOICE_RECORDED -> {
                PlayAudio(
                    state = playingState,
                    audio = inputState.audioFile,
                    playOnClick = playOnClick,
                    onClear = onClear
                )
            }
        }
    }

}

@Composable
fun InputFabs(
    inputState: InputState.State = InputState.State.INITIAL,
    onClick: () -> Unit = {},
    onVoice: () -> Unit = {},
    onCamera: () -> Unit = {},
    onGallery: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(300.dp)
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        contentAlignment = BottomCenter
    ) {
        val offsetY = remember { Animatable(0f) }
        val coroutineScope = rememberCoroutineScope()
        if (!(inputState == InputState.State.INITIAL || inputState == InputState.State.FAB_EXPAND))
            coroutineScope.launch { offsetY.snapTo(0f) }
        if (offsetY.value > 0) {
            val fabSpace = 32
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { offsetY.snapTo(0f) }
                    onVoice()
                },
                Modifier
                    .size(40.dp)
                    .offset(x = 0.dp, y = (-((56 + fabSpace) * offsetY.value).toInt()).dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = ""
                )
            }
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { offsetY.snapTo(0f) }
                    onCamera()
                },
                Modifier
                    .size(40.dp)
                    .offset(x = 0.dp, y = (-((56 + fabSpace + fabSpace + 40) * offsetY.value).toInt()).dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = ""
                )
            }
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { offsetY.snapTo(0f) }
                    onGallery()
                },
                Modifier
                    .size(40.dp)
                    .offset(x = 0.dp, y = (-((56 + fabSpace + 2 * (fabSpace + 40)) * offsetY.value).toInt()).dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = ""
                )
            }
        }
        FloatingActionButton(onClick = {
            when (inputState) {
                InputState.State.INITIAL -> {
                    coroutineScope.launch {
                        offsetY.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 200)
                        )
                        onClick()
                    }
                }
                InputState.State.FAB_EXPAND -> {
                    coroutineScope.launch {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 200)
                        )
                        onClick()
                    }
                }
                else -> onClick()
            }
        }) {
            Icon(
                imageVector = when (inputState) {
                    InputState.State.INITIAL -> Icons.Filled.Add
                    InputState.State.TEXT_ENTERED, InputState.State.VOICE_RECORDED -> Icons.Filled.PlayArrow
                    InputState.State.FAB_EXPAND -> Icons.Filled.Clear
                    InputState.State.VOICE_RECORDING -> Icons.Filled.Stop
                },
                contentDescription = ""
            )
        }
    }
}

@Preview
@Composable
fun UserNameLayout(
    userStatus: UserStatus = testDialogUiState.userStatus,
    conversation: Conversation = testDialogUiState.conversation!!,
    onClick: () -> Unit = {},
) {
    Row(modifier = Modifier
        .clickable {
            onClick()
        }) {
        val imageBitmap =
            conversation.user.photo?.let {
                getBitmapFromGallery(
                    getMiniPhotoFileName(it),
                    LocalContext.current,
                    1
                )?.asImageBitmap()
            }
        val imageModifier = Modifier
            .clip(CircleShape)
            .width(50.dp)
            .height(50.dp)
        if (imageBitmap != null) {
            Image(
                modifier = imageModifier,
                bitmap = imageBitmap,
                contentScale = ContentScale.FillWidth,
                contentDescription = ""
            )
        } else {
            Icon(
                painterResource(id = R.drawable.user_icon),
                modifier = imageModifier,
                contentDescription = ""
            )
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text(text = conversation.user.name, fontSize = 18.sp, fontWeight = Bold, color = Color.White)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = userStatus.print(LocalContext.current), fontSize = 12.sp, color = Color.White
            )
        }
    }
}

private fun UserStatus.print(context: Context) =
    when (this) {
        is UserStatus.Empty -> ""
        is UserStatus.Online -> context.getString(R.string.status_online)
        is UserStatus.Typing -> context.getString(R.string.status_typing)
        is UserStatus.LastSeen -> timeToString(this.time)
    }

@SuppressLint("SimpleDateFormat")
private fun timeToString(lastTimeOnline: Long): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(lastTimeOnline))
}

val testDialogUiState = DialogState(
    inputState = InputState(
        audioLengthText = "0:15",
        text = "Text"
    ),
    replyMessage = Message(text = "text"),
    conversation = testConversation,
    userStatus = UserStatus.Online,
)