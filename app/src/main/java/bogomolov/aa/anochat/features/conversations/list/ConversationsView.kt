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
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.paging.compose.itemKey
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.main.Route
import bogomolov.aa.anochat.features.main.theme.MyTopAppBar
import bogomolov.aa.anochat.features.main.theme.NEW_MESSAGE_COLOR
import bogomolov.aa.anochat.features.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun ConversationsView() {
    val viewModel = hiltViewModel<ConversationListViewModel>()
    viewModel.state.collectState { Content(it, viewModel) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Content(
    state: ConversationsUiState = testConversationsUiState,
    viewModel: ConversationListViewModel? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    //val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    //    if (it) navController?.navigate(Route.Users.route)
    //}
    val deleteConversation: (Long) -> Unit = remember { { viewModel?.deleteConversations(setOf(it)) } }
    val navigateConversation: (Long) -> Unit = remember { { navController?.navigate(Route.Conversation.route(it)) } }
    Scaffold(
        topBar = {
            MyTopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            onClick = remember { { navController?.navigate(Route.Settings.route) } },
                            text = {
                                Text(stringResource(id = R.string.settings))
                            }
                        )
                        DropdownMenuItem(
                            onClick = remember {
                                {
                                    viewModel?.signOut()
                                    navController?.navigate(Route.Login.route)
                                }
                            },
                            text = {
                                Text(stringResource(id = R.string.sign_out))
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = remember { {
                    //contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                    navController?.navigate(Route.Users.route)
                } }
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_contacts),
                    contentDescription = ""
                )
            }
        }) { padding ->
        if (state.pagingDataFlow != null) {
            val lazyPagingItems = state.pagingDataFlow.collectAsLazyPagingItems()
            LazyColumn(
                modifier = createInsetsModifier(padding)
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id }
                ) { index ->
                    lazyPagingItems[index]?.let {
                        ConversationCard(
                            conversation = it,
                            deleteConversation = deleteConversation,
                            navigateConversation = navigateConversation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation = testConversation,
    deleteConversation: (Long) -> Unit = {},
    navigateConversation: (Long) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(conversation.id) {
                detectTapGestures(
                    onTap = {
                        navigateConversation(conversation.id)
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
            DropdownMenuItem(
                onClick = {
                    deleteConversation(conversation.id)
                    showMenu = false
                },
                text = {
                    Text(stringResource(id = R.string.delete))
                }
            )
        }

        val isNew = conversation.lastMessage?.isMine == false && conversation.lastMessage.viewed == 0
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(60.dp)
                    .height(60.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(conversation.id) {
                    withContext(Dispatchers.IO) {
                        conversation.user.photo?.let {
                            imageBitmap = getBitmapFromGallery(getMiniPhotoFileName(it), context, 1)?.asImageBitmap()
                        }
                    }
                }
                val imageModifier = Modifier.clip(CircleShape)
                if (conversation.user.photo != null) {
                    imageBitmap?.let {
                        Image(
                            modifier = imageModifier,
                            bitmap = it,
                            contentScale = ContentScale.FillWidth,
                            contentDescription = ""
                        )
                    }
                } else {
                    Icon(
                        painterResource(id = R.drawable.user_icon),
                        modifier = imageModifier,
                        contentDescription = ""
                    )
                }
                if (isNew) {
                    Canvas(modifier = Modifier.size(12.dp), onDraw = {
                        drawCircle(color = NEW_MESSAGE_COLOR)
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
                    Text(
                        conversation.user.name,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Bold
                    )
                    conversation.lastMessage?.let {
                        Text(
                            text = it.timeString(),
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )
                    }
                }
                conversation.lastMessage?.let {
                    Text(
                        text = it.shortText(),
                        maxLines = 2,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.padding(top = 12.dp),
                        fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal,
                        color = if (isNew) NEW_MESSAGE_COLOR else Color.Unspecified
                    )
                }
            }
        }
    }
}