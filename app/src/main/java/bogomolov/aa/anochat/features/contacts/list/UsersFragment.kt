package bogomolov.aa.anochat.features.contacts.list

import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.entity.isValidPhone
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class UsersFragment : Fragment() {
    private val viewModel: UsersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadContacts(getContactsPhones())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                val state = viewModel.state.collectAsState()
                Content(state.value)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.events.collect {
                if (it is NavigateConversationEvent) navigateToConversation(it.conversationId)
            }
        }
    }


    @Preview
    @Composable
    private fun Content(state: ContactsUiState = testContactsUiState) {
        MaterialTheme(
            colors = LightColorPalette
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.contacts)) },
                        navigationIcon = {
                            IconButton(onClick = {
                                findNavController().popBackStack()
                            }) {
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
                            state.users?.forEach { UserRow(it) }
                        }
                    }
                }
            )
        }
    }


    @Composable
    private fun UserRow(user: User = testContactsUiState.users!!.first()) {
        Card(
            backgroundColor = Color.Black.copy(alpha = 0.0f),
            elevation = 0.dp,
            modifier = Modifier.clickable(onClick = {
                viewModel.createConversation(user)
            })
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

    private fun navigateToConversation(conversationId: Long) {
        if (conversationId != 0L) {
            val uri = arguments?.getString("uri")
            val bundle = Bundle().apply {
                putLong("id", conversationId)
                if (uri != null) putString("uri", uri)
            }
            findNavController().navigate(R.id.dialog_graph, bundle)
        }
    }

    private fun getContactsPhones(): List<String> {
        val phones = HashSet<String>()
        val cursor = queryContacts()
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

    private fun queryContacts(): Cursor? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        return requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )
    }

    /*
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.users_menu, menu)
        val searchView = SearchView(requireContext())
        menu.findItem(R.id.action_search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem) = true

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    viewModel.resetSearch()
                    return true
                }
            })
        }
        searchView.setTextColor(R.color.title_color)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) =
                if (query != null) {
                    viewModel.search(query)
                    true
                } else false

            override fun onQueryTextChange(newText: String?) = false
        })
        val closeButton = searchView.findViewById(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            menu.findItem(R.id.action_search).collapseActionView()
            viewModel.resetSearch()
        }
    }
     */
}