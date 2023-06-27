package bogomolov.aa.anochat.features.settings

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.main.Route
import bogomolov.aa.anochat.features.main.theme.MyTopAppBar
import bogomolov.aa.anochat.features.shared.*
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun SettingsView() {
    val navController = LocalNavController.current
    val backStackEntry = remember { navController!!.getBackStackEntry(Route.Settings.navGraphRoute) }
    val viewModel = hiltViewModel<SettingsViewModel>(backStackEntry)
    viewModel.events.collectEvents {
        if (it is PhotoResizedEvent) navController?.navigate(Route.Miniature.route)
    }
    viewModel.state.collectState { Content(it, viewModel) }
}

@ExperimentalMaterial3Api
@Preview
@Composable
private fun Content(
    state: SettingsUiState = testSettingsUiState,
    viewModel: SettingsViewModel? = null
) {
    val navController = LocalNavController.current
    val fileChooser = rememberLauncherForActivityResult(StartFileChooser()) { uri ->
        uri?.let { viewModel?.resizePhoto(it) }
    }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        fileChooser.launch(Unit)
    }
    val writePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val bottomSheetState = rememberBottomSheetScaffoldState(bottomSheetState = rememberStandardBottomSheetState(skipHiddenState = false))
    val coroutineScope = rememberCoroutineScope()
    val collapse: () -> Unit = remember { { coroutineScope.launch { bottomSheetState.bottomSheetState.hide() } } }
    val expand: () -> Unit = remember { { coroutineScope.launch { bottomSheetState.bottomSheetState.expand() } } }
    val settingsText = rememberSaveable { mutableStateOf("") }
    val settingsEditType = rememberSaveable { mutableStateOf<SettingEditType?>(null) }
    val updateSettings: (Settings.() -> Settings) -> Unit = remember { { viewModel?.updateSettings(it) } }
    val updateUser: (User.() -> User) -> Unit = remember { { viewModel?.updateUser(it) } }
    BottomSheetScaffold(
        modifier = InsetsModifier,
        sheetPeekHeight = 0.dp,
        scaffoldState = bottomSheetState,
        sheetContent = {
            BottomSheetContent(
                settingsText = settingsText,
                settingsEditType = settingsEditType,
                collapse = collapse,
                updateUser = updateUser
            )
        },
        topBar = {
            MyTopAppBar(
                title = { Text(stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = remember { { navController?.popBackStack() } }
                    ) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        sheetContainerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .fillMaxSize()
        ) {
            if (state.user == null)
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                )
            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                UserPhotoCompose(
                    user = state.user,
                    askReadPermission = remember { { readPermission.launch(READ_EXTERNAL_STORAGE) } }
                )
                UserInfoCompose(
                    user = state.user,
                    expand = { type, text ->
                        settingsText.value = text
                        settingsEditType.value = type
                        expand()
                    }
                )
            }
            SettingsCompose(
                settings = state.settings,
                askWritePermission = remember { { writePermission.launch(WRITE_EXTERNAL_STORAGE) } },
                updateSettings = updateSettings
            )
        }
    }
}

@Composable
private fun UserInfoCompose(
    user: User? = null,
    expand: (SettingEditType, String) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 16.dp)
    ) {
        Text(
            text = user?.phone ?: "",
            fontSize = MaterialTheme.typography.titleMedium.fontSize
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = user?.name ?: "",
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "",
                modifier = Modifier.clickable {
                    user?.let { expand(SettingEditType.EDIT_USERNAME, it.name) }
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                user?.status ?: "",
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
            )
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "",
                modifier = Modifier.clickable {
                    user?.status?.let { expand(SettingEditType.EDIT_STATUS, it) }
                }
            )
        }
    }
}

@Composable
private fun UserPhotoCompose(
    user: User? = null,
    askReadPermission: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        user?.let {
            val imageModifier = Modifier.clip(CircleShape)
            it.photo?.let {
                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(it) {
                    imageBitmap = getBitmapFromGallery(
                        getMiniPhotoFileName(it),
                        context,
                        1
                    )?.asImageBitmap()
                }
                imageBitmap?.let {
                    Image(
                        modifier = imageModifier,
                        bitmap = it,
                        contentScale = ContentScale.FillWidth,
                        contentDescription = ""
                    )
                }
            } ?: run {
                Icon(
                    painterResource(id = R.drawable.user_icon),
                    modifier = imageModifier,
                    contentDescription = ""
                )
            }
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "",
                modifier = Modifier.clickable(
                    onClick = { askReadPermission() }
                )
            )
        }
    }
}

@Composable
private fun SettingsCompose(
    settings: Settings,
    askWritePermission: () -> Unit,
    updateSettings: (Settings.() -> Settings) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        SettingRow(
            nameRes = R.string.notifications,
            checked = settings.notifications,
            paddingTop = 0.dp
        ) { updateSettings { copy(notifications = it) } }
        SettingRow(
            nameRes = R.string.sound,
            checked = settings.sound
        ) { updateSettings { copy(sound = it) } }
        SettingRow(
            nameRes = R.string.vibration,
            checked = settings.vibration
        ) { updateSettings { copy(vibration = it) } }
        SettingRow(
            nameRes = R.string.save_to_gallery,
            checked = settings.gallery
        ) {
            askWritePermission()
            updateSettings { copy(gallery = it) }
        }
        Text(
            text = stringResource(id = R.string.privacy_policy),
            modifier = Modifier
                .padding(top = 32.dp)
                .clickable { openPrivacyPolicy(context) }
        )
    }
}

@Composable
private fun BottomSheetContent(
    settingsText: MutableState<String> = mutableStateOf(""),
    settingsEditType: MutableState<SettingEditType?> = mutableStateOf(SettingEditType.EDIT_STATUS),
    collapse: () -> Unit,
    updateUser: (User.() -> User) -> Unit
) {
    Column(Modifier.navigationBarsPadding().imePadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            TextField(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                value = settingsText.value,
                onValueChange = { settingsText.value = it },
                label = {
                    settingsEditType.value?.let {
                        Text(
                            text = stringResource(
                                when (it) {
                                    SettingEditType.EDIT_USERNAME -> R.string.enter_new_name
                                    SettingEditType.EDIT_STATUS -> R.string.enter_new_status
                                }
                            )
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
        Row(
            Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    settingsEditType.value?.let {
                        if (settingsText.value.isNotEmpty())
                            when (it) {
                                SettingEditType.EDIT_USERNAME -> updateUser { copy(name = settingsText.value) }
                                SettingEditType.EDIT_STATUS -> updateUser { copy(status = settingsText.value) }
                            }
                    }
                    settingsEditType.value = null
                    collapse()
                }
            ) {
                Text(text = stringResource(id = R.string.save))
            }
            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    settingsEditType.value = null
                    collapse()
                }
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    }
}

@Composable
private fun SettingRow(nameRes: Int, checked: Boolean, paddingTop: Dp = 32.dp, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = paddingTop),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = stringResource(id = nameRes))
        Switch(
            checked = checked,
            onCheckedChange = onChecked
        )
    }
}

private fun openPrivacyPolicy(context: Context) {
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(context.resources.getString(R.string.privacy_policy_url))
    context.startActivity(i)
}

private class StartFileChooser : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        return Intent.createChooser(intent, context.getString(R.string.select_file))
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.data
}