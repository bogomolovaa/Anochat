package bogomolov.aa.anochat.features.settings

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.shared.LightColorPalette
import bogomolov.aa.anochat.features.shared.getBitmapFromGallery
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import bogomolov.aa.anochat.repository.FileStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    val viewModel: SettingsViewModel by hiltNavGraphViewModels(R.id.settings_graph)

    @Inject
    lateinit var fileStore: FileStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                val state = viewModel.state.collectAsState()
                Content(state.value)
            }
        }

    @Preview
    @Composable
    private fun Content(state: SettingsUiState = testSettingsUiState) {
        MaterialTheme(
            colors = LightColorPalette
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.settings)) },
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
                        if (state.user == null) LinearProgressIndicator(modifier = Modifier.padding(top = 4.dp))
                        Row(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                val imageBitmap = state.user?.photo?.let {
                                    getBitmapFromGallery(
                                        getMiniPhotoFileName(it),
                                        LocalContext.current,
                                        1
                                    )?.asImageBitmap()
                                }
                                val imageModifier = Modifier
                                    .clip(CircleShape)
                                    .width(100.dp)
                                    .height(100.dp)
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
                                Icon(
                                    painterResource(id = R.drawable.edit_icon),
                                    contentDescription = "",
                                    modifier = Modifier.clickable {
                                        if (state.user != null) readPermission.launch(READ_EXTERNAL_STORAGE)
                                    }
                                )
                            }
                            Column(
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Text(state.user?.phone ?: "", fontSize = 16.sp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        state.user?.name ?: "",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Icon(
                                        painterResource(id = R.drawable.edit_icon),
                                        contentDescription = "",
                                        modifier = Modifier.clickable {
                                            showEditNameDialog(state.user?.name)
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
                                        state.user?.status ?: "",
                                        fontSize = 16.sp,
                                    )
                                    Icon(
                                        painterResource(id = R.drawable.edit_icon),
                                        contentDescription = "",
                                        modifier = Modifier.clickable {
                                            showEditStatusDialog(state.user?.status)
                                        }
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            SettingRow(
                                nameRes = R.string.notifications,
                                checked = state.settings.notifications,
                                paddingTop = 0.dp
                            ) { viewModel.changeSettings { copy(notifications = it) } }
                            SettingRow(
                                nameRes = R.string.sound,
                                checked = state.settings.sound
                            ) { viewModel.changeSettings { copy(sound = it) } }
                            SettingRow(
                                nameRes = R.string.vibration,
                                checked = state.settings.vibration
                            ) { viewModel.changeSettings { copy(vibration = it) } }
                            SettingRow(
                                nameRes = R.string.save_to_gallery,
                                checked = state.settings.gallery
                            ) {
                                viewModel.changeSettings {
                                    writePermission.launch(WRITE_EXTERNAL_STORAGE)
                                    copy(gallery = it)
                                }
                            }
                            Text(
                                text = stringResource(id = R.string.privacy_policy),
                                modifier = Modifier
                                    .padding(top = 32.dp)
                                    .clickable { openPrivacyPolicy() }
                            )
                        }
                    }
                }
            )
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

    private fun showEditNameDialog(name: String?) {
        val bottomSheetFragment = EditUserBottomDialogFragment(
            SettingType.EDIT_USERNAME,
            requireContext().resources.getString(R.string.enter_new_name),
            name
        ) {
            if (it.isNotEmpty()) viewModel.updateUser { copy(name = it) }
        }
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
    }

    private fun showEditStatusDialog(status: String?) {
        val bottomSheetFragment = EditUserBottomDialogFragment(
            SettingType.EDIT_STATUS,
            requireContext().resources.getString(R.string.enter_new_status),
            status
        ) {
            if (it.isNotEmpty()) viewModel.updateUser { copy(status = it) }
        }
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
    }

    private fun openPrivacyPolicy() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(requireContext().resources.getString(R.string.privacy_policy_url))
        startActivity(i)
    }

    private fun updatePhoto(uri: Uri) {
        val miniature = fileStore.resizeImage(uri = uri, toGallery = false)
        if (miniature != null) {
            viewModel.miniature = miniature
            findNavController().navigate(R.id.miniatureFragment)
        }
    }

    private val fileChooser = registerForActivityResult(StartFileChooser()) { uri ->
        if (uri != null) updatePhoto(uri)
    }

    private val readPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            fileChooser.launch(Unit)
        }

    private val writePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
}

private class StartFileChooser : ActivityResultContract<Unit, Uri>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        return Intent.createChooser(intent, context.getString(R.string.select_file))
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.data
}