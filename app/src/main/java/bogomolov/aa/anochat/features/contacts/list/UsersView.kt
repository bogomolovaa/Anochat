package bogomolov.aa.anochat.features.contacts.list

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.entity.isValidPhone
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.main.Route
import bogomolov.aa.anochat.features.main.theme.MyTopAppBar
import bogomolov.aa.anochat.features.shared.*
import java.net.URLEncoder

@Composable
fun UsersView(uri: String? = null) {
    val viewModel = hiltViewModel<UsersViewModel>()
    val navController = LocalNavController.current
    val context = LocalContext.current
    LaunchedEffect(0) {
        //viewModel.loadContacts(getContactsPhones(context))
        viewModel.loadContacts(listOf())
    }
    viewModel.events.collectEvents {
        if (it is NavigateConversationEvent) navController?.navigateToConversation(it.conversationId, uri)
    }
    viewModel.state.collectState {
        Content(
            state = it,
            viewModel = viewModel,
            doBack = remember { { navController?.popBackStack() } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Content(
    state: ContactsUiState = testContactsUiState,
    viewModel: UsersViewModel? = null,
    doBack: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    Scaffold(
        topBar = {
            MyTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.search == null) {
                            Text(stringResource(id = R.string.contacts))
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = remember { { viewModel?.search() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search"
                                )
                            }
                        } else {
                            TextField(
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                value = state.search.text,
                                placeholder = { Text(stringResource(id = R.string.search_phone_placeholder)) },
                                onValueChange = remember { { viewModel?.search(it) } },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                )
                            )
                            IconButton(
                                onClick = remember { { viewModel?.search(null) } }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                            SideEffect {
                                focusRequester.requestFocus()
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = doBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        content = { padding ->
            Column(
                modifier = createInsetsModifier(padding)
                    .fillMaxWidth()
            ) {
                if (state.loading)
                    LinearProgressIndicator(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                    )
                state.users?.let { users ->
                    LazyColumn(
                        modifier = Modifier.padding(top = if (!state.loading) 8.dp else 0.dp)
                    ) {
                        items(count = users.size) {
                            UserRow(
                                user = users[it],
                                createConversation = remember { { viewModel?.createConversation(it) } }
                            )
                        }
                    }
                }
            }
        }
    )
}


@Composable
private fun UserRow(
    user: User = testContactsUiState.users!!.first(),
    createConversation: (User) -> Unit = {}
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = { createConversation(user) })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)

        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .width(60.dp)
                    .height(60.dp)
            ) {

                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(user.id) {
                    user.photo?.let {
                        imageBitmap = getBitmapFromGallery(
                            getMiniPhotoFileName(it),
                            context,
                            1
                        )?.asImageBitmap()
                    }
                }
                if (user.photo != null) {
                    imageBitmap?.let {
                        Image(
                            bitmap = it,
                            contentScale = ContentScale.FillWidth,
                            contentDescription = ""
                        )
                    }
                } else {
                    Icon(
                        painterResource(id = R.drawable.user_icon),
                        contentDescription = ""
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(user.name, fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold)
                Text(user.phone ?: "")
                Text(user.status ?: "")
            }
        }
    }

}

private fun NavController.navigateToConversation(conversationId: Long, uri: String?) {
    navigate(Route.Conversation.route(conversationId, URLEncoder.encode(uri.toString(), "utf-8")))
}

private fun getContactsPhones(context: Context): List<String> {
    val phones = HashSet<String>()
    val cursor = context.queryContacts()
    if (cursor != null) {
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
            val number = cursor.getString(numberIndex)
            val clearNumber =
                number.replace("[- ()]".toRegex(), "").replace("^8".toRegex(), "+7")
            if (isValidPhone(clearNumber)) phones += clearNumber
        }
    }
    return ArrayList(phones)
}

private fun Context.queryContacts(): Cursor? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    return contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        null
    )
}