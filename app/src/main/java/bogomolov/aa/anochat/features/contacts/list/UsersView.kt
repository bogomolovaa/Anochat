package bogomolov.aa.anochat.features.contacts.list

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.entity.isValidPhone
import bogomolov.aa.anochat.features.main.Navigation
import bogomolov.aa.anochat.features.shared.EventHandler
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName

@Composable
fun UsersView(uri: String? = null) {
    val viewModel = hiltViewModel<UsersViewModel>()
    val context = LocalContext.current
    LaunchedEffect(0) {
        viewModel.loadContacts(getContactsPhones(context))
    }
    EventHandler(viewModel.events) {
        if (it is NavigateConversationEvent) Navigation.navController?.navigateToConversation(it.conversationId, uri)
    }

    val state = viewModel.state.collectAsState()
    Content(state.value, viewModel)
}

@Preview
@Composable
private fun Content(state: ContactsUiState = testContactsUiState, viewModel: UsersViewModel? = null) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.contacts)) },
                navigationIcon = {
                    IconButton(onClick = { Navigation.navController?.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) LinearProgressIndicator(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                )
                Column(
                    modifier = Modifier.padding(top = if (!state.loading) 8.dp else 0.dp)
                ) {
                    state.users?.forEach { UserRow(it, viewModel) }
                }
            }
        }
    )
}


@Composable
private fun UserRow(user: User = testContactsUiState.users!!.first(), viewModel: UsersViewModel? = null) {
    Card(
        backgroundColor = Color.Black.copy(alpha = 0.0f),
        elevation = 0.dp,
        modifier = Modifier.clickable(onClick = { viewModel?.createConversation(user) })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)

        ) {
            val imageBitmap =
                user.photo?.let {
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
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(user.phone ?: "")
                Text(user.status ?: "")
            }
        }
    }

}

private fun NavController.navigateToConversation(conversationId: Long, uri: String?) {
    navigate("conversation?id=$conversationId" + if (uri != null) "&uri=$uri" else "")
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

/*
   viewModel.resetSearch()
   viewModel.search(query)
*/