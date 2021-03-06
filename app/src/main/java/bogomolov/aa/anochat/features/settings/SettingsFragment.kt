package bogomolov.aa.anochat.features.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentSettingsBinding
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import bogomolov.aa.anochat.features.shared.resizeImage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(), UpdatableView<SettingsUiState> {
    private val viewModel: SettingsViewModel by hiltNavGraphViewModels(R.id.settings_graph)
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.addAction(LoadSettingsAction())
        viewModel.addAction(LoadMyUserAction())
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        addListeners()

        return binding.root
    }

    override fun updateView(newState: SettingsUiState, currentState: SettingsUiState) {
        binding.notificationsSwitch.isChecked = newState.settings.notifications
        binding.soundSwitch.isChecked = newState.settings.sound
        binding.vibrationSwitch.isChecked = newState.settings.vibration
        binding.gallerySwitch.isChecked = newState.settings.gallery
        if (newState.user != null) {
            binding.progressBar.visibility = View.INVISIBLE
            if (newState.user.photo != currentState.user?.photo)
                binding.userPhoto.setImage(newState.user.photo!!)
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
            if (it.isNotEmpty()) viewModel.addAction(UpdateNameAction(it))
        }
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
    }

    private fun showEditStatusDialog(status: String?) {
        val bottomSheetFragment = EditUserBottomDialogFragment(
            SettingType.EDIT_STATUS,
            requireContext().resources.getString(R.string.enter_new_status),
            status
        ) {
            if (it.isNotEmpty()) viewModel.addAction(UpdateStatusAction(it))
        }
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
    }

    private fun addListeners() {
        binding.privacyPolicy.setOnClickListener { openPrivacyPolicy() }
        binding.editPhoto.setOnClickListener { if (viewModel.state.user != null) requestReadPermission() }
        binding.gallerySwitch.setOnClickListener { requestWritePermission() }
        observeChangesFor(binding.notificationsSwitch) { checked -> copy(notifications = checked) }
        observeChangesFor(binding.soundSwitch) { checked -> copy(sound = checked) }
        observeChangesFor(binding.vibrationSwitch) { checked -> copy(vibration = checked) }
        observeChangesFor(binding.gallerySwitch) { checked -> copy(gallery = checked) }
    }

    private fun observeChangesFor(switch: Switch, change: Settings.(Boolean) -> Settings) {
        switch.setOnCheckedChangeListener { _, checked ->
            viewModel.addAction(ChangeSettingsAction { change(checked) })
        }
    }

    private fun openPrivacyPolicy() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(requireContext().resources.getString(R.string.privacy_policy_url))
        startActivity(i)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == FILE_CHOOSER_CODE) {
            val uri = intent?.data
            if (uri != null) updatePhoto(uri)
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun updatePhoto(uri: Uri) {
        val miniature = resizeImage(uri = uri, context = requireContext(), toGallery = false)
        if (miniature != null) {
            viewModel.miniature = miniature
            navController.navigate(R.id.miniatureFragment)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            if (requestCode == READ_PERMISSIONS_CODE) startFileChooser()
    }

    private fun startFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_file)),
                FILE_CHOOSER_CODE
            )
        } catch (ex: ActivityNotFoundException) {
            Log.w("SettingFragment", "File manager not installed")
        }
    }

    private fun requestReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(READ_PERMISSION), READ_PERMISSIONS_CODE)
    }

    private fun requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            requestPermissions(arrayOf(WRITE_PERMISSION), WRITE_PERMISSIONS_CODE)
    }

    companion object {
        private const val FILE_CHOOSER_CODE: Int = 0
        private const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val READ_PERMISSIONS_CODE = 1001
        private const val WRITE_PERMISSIONS_CODE = 1002
    }
}