package bogomolov.aa.anochat.features.settings

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentSettingsBinding
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.bindingDelegate
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import bogomolov.aa.anochat.repository.FileStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings), UpdatableView<SettingsUiState> {
    val viewModel: SettingsViewModel by hiltNavGraphViewModels(R.id.settings_graph)
    private val binding by bindingDelegate(FragmentSettingsBinding::bind)
    private lateinit var navController: NavController

    @Inject
    lateinit var fileStore: FileStore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        addListeners()
    }

    override fun updateView(newState: SettingsUiState, currentState: SettingsUiState) {
        binding.notificationsSwitch.isChecked = newState.settings.notifications
        binding.soundSwitch.isChecked = newState.settings.sound
        binding.vibrationSwitch.isChecked = newState.settings.vibration
        binding.gallerySwitch.isChecked = newState.settings.gallery
        if (newState.user != null) {
            binding.progressBar.visibility = View.INVISIBLE
            if (newState.user.photo != null && newState.user.photo != currentState.user?.photo)
                binding.userPhoto.setImage(newState.user.photo)
            if (newState.user.name != currentState.user?.name) {
                binding.usernameText.text = newState.user.name
                binding.editUsername.setOnClickListener { showEditNameDialog(newState.user.name) }
            }
            if (newState.user.status != currentState.user?.status) {
                binding.statusText.text =
                    newState.user.status ?: requireContext().resources.getText(R.string.no_status)
                binding.editStatus.setOnClickListener { showEditStatusDialog(newState.user.status) }
            }
            if (newState.user.phone != currentState.user?.phone)
                binding.phoneText.text = newState.user.phone
        } else {
            binding.progressBar.visibility = View.VISIBLE
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

    private fun addListeners() {
        binding.privacyPolicy.setOnClickListener { openPrivacyPolicy() }
        binding.editPhoto.setOnClickListener {
            if (viewModel.currentState.user != null) readPermission.launch(READ_EXTERNAL_STORAGE)
        }
        binding.gallerySwitch.setOnClickListener { writePermission.launch(WRITE_EXTERNAL_STORAGE) }
        observeChangesFor(binding.notificationsSwitch) { checked -> copy(notifications = checked) }
        observeChangesFor(binding.soundSwitch) { checked -> copy(sound = checked) }
        observeChangesFor(binding.vibrationSwitch) { checked -> copy(vibration = checked) }
        observeChangesFor(binding.gallerySwitch) { checked -> copy(gallery = checked) }
    }

    private fun observeChangesFor(switch: Switch, change: Settings.(Boolean) -> Settings) {
        switch.setOnCheckedChangeListener { _, checked ->
            viewModel.changeSettings { change(checked) }
        }
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
            navController.navigate(R.id.miniatureFragment)
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