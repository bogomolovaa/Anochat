package bogomolov.aa.anochat.features.contacts.list

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
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
import bogomolov.aa.anochat.features.shared.EventHandler
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
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
    EventHandler(viewModel.events) {
        if (it is NavigateConversationEvent) navController?.navigateToConversation(it.conversationId, uri)
    }

    val state = viewModel.state.collectAsState()
    Content(
        state = state.value,
        viewModel = viewModel,
        doBack = remember { { navController?.popBackStack() } }
    )
}

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
            TopAppBar(
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
                                colors = TextFieldDefaults.textFieldColors(
                                    cursorColor = Color.White,
                                    backgroundColor = Color.Transparent,
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
                modifier = Modifier
                    .padding(padding)
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
        backgroundColor = Color.Black.copy(alpha = 0.0f),
        elevation = 0.dp,
        modifier = Modifier.clickable(onClick = { createConversation(user) })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)

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
            val imageModifier = Modifier
                .clip(CircleShape)
                .width(60.dp)
                .height(60.dp)
            imageBitmap?.let {
                Image(
                    modifier = imageModifier,
                    bitmap = it,
                    contentScale = ContentScale.FillWidth,
                    contentDescription = ""
                )
            } ?: run {
                Icon(
                    painterResource(id = R.drawable.user_icon),
                    modifier = imageModifier,
                    contentDescription = ""
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(user.phone ?: "")
                Text(user.status ?: "")
            }
        }
    }

}

private fun NavController.navigateToConversation(conversationId: Long, uri: String?) {
    navigate(
        "conversation?id=$conversationId" + if (uri != null) "&uri=${
            URLEncoder.encode(uri.toString(), "utf-8")
        }" else ""
    )
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