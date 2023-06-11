package bogomolov.aa.anochat.features.conversations.list

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName


@Composable
fun ConversationsView() {
    val viewModel = hiltViewModel<ConversationListViewModel>()
    val state = viewModel.state.collectAsState()
    Content(state.value, viewModel)
}

@Composable
private fun Content(
    state: ConversationsUiState = testConversationsUiState,
    viewModel: ConversationListViewModel?
) {
    var showMenu by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    val contactsPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) navController?.navigate("users")
        }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            navController?.navigate("settings")
                        }) {
                            Text(stringResource(id = R.string.settings))
                        }
                        DropdownMenuItem(onClick = {
                            viewModel?.signOut()
                            navController?.navigate("login")
                        }) {
                            Text(stringResource(id = R.string.sign_out))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val context = LocalContext.current
            FloatingActionButton(onClick = {
                contactsPermission.launch(Manifest.permission.READ_CONTACTS)
            }) {
                Icon(
                    painterResource(id = R.drawable.ic_contacts),
                    contentDescription = ""
                )
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier.padding(padding)
            ) {
                if (state.pagingDataFlow != null) {
                    val lazyPagingItems = state.pagingDataFlow.collectAsLazyPagingItems()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(count = lazyPagingItems.itemCount) { index ->
                            lazyPagingItems[index]?.let { ConversationCard(it, viewModel) }
                        }
                    }
                }
            }
        }

    )
}

@Preview
@Composable
private fun ConversationCard(
    conversation: Conversation = testConversation,
    viewModel: ConversationListViewModel? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    Card(
        backgroundColor = Color.Black.copy(alpha = 0.0f),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        navController?.navigate("conversation?id=${conversation.id}")
                    },
                    onLongPress = { showMenu = true }
                )
            }
            .padding(start = 12.dp, end = 12.dp)

    ) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(70.dp, -60.dp)
        ) {
            DropdownMenuItem(onClick = {
                viewModel?.deleteConversations(setOf(conversation.id))
                showMenu = false
            }) {
                Text(stringResource(id = R.string.delete))
            }
        }

        val isNew =
            conversation.lastMessage?.isMine == false && conversation.lastMessage?.viewed == 0
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.TopEnd) {
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
                    .width(60.dp)
                    .height(60.dp)
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
                if (isNew) {
                    val color = colorResource(R.color.green)
                    Canvas(modifier = Modifier.size(12.dp), onDraw = {
                        drawCircle(color = color)
                    })
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(conversation.user.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    conversation.lastMessage?.let { Text(text = it.timeString(), fontSize = 12.sp) }
                }
                conversation.lastMessage?.let {
                    Text(
                        text = it.shortText(),
                        maxLines = 2,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 12.dp),
                        fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal,
                        color = if (isNew) colorResource(R.color.green) else Color.Black
                    )
                }
            }
        }
    }
}