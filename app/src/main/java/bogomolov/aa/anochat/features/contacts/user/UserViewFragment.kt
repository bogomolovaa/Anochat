package bogomolov.aa.anochat.features.contacts.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.transition.Fade
import androidx.transition.Transition
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.getBitmap
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow

@AndroidEntryPoint
class UserViewFragment : Fragment() {
    private val viewModel: UserViewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = arguments?.getLong("id")!!
        viewModel.initUser(userId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                val state = viewModel.state.collectAsState()
                Content(state.value)
            }
        }

    @Preview
    @Composable
    private fun Content(state: UserUiState = testUserUiState) {
        MaterialTheme(
            colors = LightColorPalette
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(state.user?.name ?: "") },
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
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
                    ) {
                        getBitmap(state.user?.photo, LocalContext.current)?.asImageBitmap()?.let {
                            Image(
                                bitmap = it,
                                contentDescription = "user image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .clickable(onClick = {
                                        val photo = state.user?.photo
                                        if (photo != null) {
                                            val bundle = Bundle().apply {
                                                putString("image", photo)
                                                putInt("quality", 1)
                                            }
                                            findNavController().navigate(R.id.imageViewFragment, bundle)
                                        }
                                    })
                            )
                        } ?: run {
                            Icon(
                                painterResource(id = R.drawable.user_icon),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentDescription = ""
                            )
                        }
                        if (state.pagingFlow != null) ImagesRow(pagingFlow = state.pagingFlow)
                        Text("${state.user?.phone}", modifier = Modifier.padding(16.dp))
                        Text("${state.user?.status}", modifier = Modifier.padding(16.dp))
                    }
                }
            )
        }
    }

    @Composable
    private fun ImagesRow(pagingFlow: Flow<PagingData<String>>) {
        val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(start = 5.dp, top = 5.dp, end = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lazyPagingItems) { image ->
                Card {
                    val imageBitmap = getBitmapFromGallery(image, LocalContext.current, 8)?.asImageBitmap()
                    if (imageBitmap != null)
                        Image(
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .clickable(onClick = {
                                    //val extras =
                                    //    FragmentNavigator.Extras.Builder().addSharedElement(imageView, image).build()
                                    findNavController().navigate(
                                        R.id.imageViewFragment,
                                        Bundle().apply {
                                            putString("image", image)
                                            putInt("quality", 1)
                                            putBoolean("gallery", true)
                                        }
                                    )
                                }),
                            bitmap = imageBitmap,
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                        )
                }
            }
        }
    }
}