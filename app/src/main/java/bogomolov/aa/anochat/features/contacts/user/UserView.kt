package bogomolov.aa.anochat.features.contacts.user


import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun UserView(userId: Long) {
    val viewModel = hiltViewModel<UserViewViewModel>()
    LaunchedEffect(0) {
        viewModel.initUser(userId)
    }
    collectState(viewModel.state) { Content(it, viewModel) }
}

@Preview
@Composable
private fun Content(
    state: UserUiState = testUserUiState,
    viewModel: UserViewViewModel? = null
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val navigate: (String) -> Unit = remember { { navController?.navigate("image?name=$it") } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.user?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = remember { { navController?.popBackStack() } }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                state.user?.let {
                    if (it.photo != null) {
                        val photo = nameToImage(it.photo)
                        var photoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                        LaunchedEffect(0) {
                            withContext(Dispatchers.IO) {
                                photoBitmap = getBitmap(photo, context)?.asImageBitmap()
                            }
                        }
                        photoBitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = "user image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .clickable(onClick = { navigate(photo) })
                            )
                        }
                    } else {
                        Icon(
                            painterResource(id = R.drawable.user_icon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentDescription = ""
                        )
                    }
                }
                viewModel?.pagingFlow?.let {
                    ImagesRow(
                        pagingFlow = it,
                        navigate = navigate
                    )
                }
                Text("${state.user?.phone}", modifier = Modifier.padding(16.dp))
                Text("${state.user?.status}", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun ImagesRow(
    pagingFlow: ImmutableFlow<PagingData<String>>,
    navigate: (String) -> Unit
) {
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(start = 5.dp, top = 5.dp, end = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it }
        ) { index ->
            lazyPagingItems[index]?.let {
                ImageCompose(image = it, navigate = navigate)
            }
        }
    }
}

@Composable
private fun ImageCompose(
    image: String = "",
    navigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(image) {
        withContext(Dispatchers.IO) {
            delay(10)
            imageBitmap.value = getBitmapFromGallery(image, context, 4)?.asImageBitmap()
        }
    }
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp)
            .clickable(onClick = { navigate(image) })
    ) {
        imageBitmap.value?.let {
            Image(
                bitmap = it,
                contentDescription = "",
                contentScale = ContentScale.Crop,
            )
        }
    }
}