package bogomolov.aa.anochat.features.conversations.list

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ConversationListFragment : Fragment() {
    val viewModel: ConversationListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                val state = viewModel.state.collectAsState()
                Content(state.value)
            }
        }

    @Composable
    private fun Content(state: ConversationsUiState = testConversationsUiState) {
        MaterialTheme(
            colors = LightColorPalette
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.app_name)) },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        requestContactsPermission()
                    }) {
                        Icon(
                            painterResource(id = R.drawable.ic_contacts),
                            contentDescription = ""
                        )
                    }
                },
                content = {
                    Column(
                    ) {
                        if (state.pagingDataFlow != null) {
                            val lazyPagingItems = state.pagingDataFlow.collectAsLazyPagingItems()
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(lazyPagingItems) { ConversationCard(it!!) }
                            }
                        }
                    }
                }

            )
        }
    }

    @Preview
    @Composable
    private fun ConversationCard(conversation: Conversation = testConversation) {
        Card(
            backgroundColor = Color.Black.copy(alpha = 0.0f),
            elevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    val bundle = Bundle().apply { putLong("id", conversation.id) }
                    findNavController().navigate(R.id.dialog_graph, bundle)
                })
                .padding(start = 12.dp, end = 12.dp)

        ) {
            val isNew = conversation.lastMessage?.isMine == false && conversation.lastMessage?.viewed == 0
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
                            text = it.text,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 12.dp),
                            fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal,
                            color = if (isNew) colorResource(R.color.green) else Color.Black
                        )
                    }
                }
            }
        }
    }


    /*
        data.actionsMap[R.id.delete_conversations_action] =
            { ids, _ -> viewModel.deleteConversations(ids) }

                if (it.itemId == R.id.menu_sign_out) {
                    viewModel.signOut()
                    navController.navigate(R.id.signInFragment)
                    true
                } else {
                    NavigationUI.onNavDestinationSelected(it, navController)
                }

                searchView.setOnSubmitListener { query ->
                    val bundle = Bundle().apply { putString("search", query) }
                    findNavController().navigate(R.id.messageSearchFragment, bundle)
                }

    override fun onItemSelected(binding: ConversationLayoutBinding, selected: Boolean) {
        val color = if (selected)
            ContextCompat.getColor(context, R.color.not_my_message_color)
        else
            ContextCompat.getColor(context, R.color.conversation_background)
    }
     */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            if (requestCode == CONTACTS_PERMISSIONS_CODE) findNavController().navigate(R.id.usersFragment)
    }

    private fun requestContactsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(CONTACTS_PERMISSIONS), CONTACTS_PERMISSIONS_CODE)
    }

    companion object {
        private const val CONTACTS_PERMISSIONS = Manifest.permission.READ_CONTACTS
        private const val CONTACTS_PERMISSIONS_CODE = 1001
    }
}