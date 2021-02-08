package bogomolov.aa.anochat.features.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSettingsBinding
import bogomolov.aa.anochat.features.shared.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.UpdatableView
import bogomolov.aa.anochat.repository.*
import bogomolov.aa.anochat.view.fragments.EditUserBottomDialogFragment
import bogomolov.aa.anochat.view.fragments.SettingType
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class SettingsFragment : Fragment(), UpdatableView<SettingsUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: SettingsViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var navController: NavController

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = getSetting<String>(requireContext(), UID)
        viewModel.addAction(LoadUserAction(uid!!))
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_settings,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        addListeners()

        return binding.root
    }

    override fun updateView(newState: SettingsUiState, currentState: SettingsUiState) {
        Log.i("SettingsFragment", "updateView newState:\n${newState}\ncurrentState:\n$currentState")
        if (newState.user != null) {
            Log.i("SettingsFragment","newState.user not null")
            if (newState.user.photo != currentState.user?.photo)
                binding.userPhoto.setFile(newState.user.photo!!)
            if (newState.user.name != currentState.user?.name) {
                Log.i("SettingsFragment","diff name")
                binding.usernameText.text = newState.user.name
                binding.editUsername.setOnClickListener {
                    val bottomSheetFragment = EditUserBottomDialogFragment(
                        SettingType.EDIT_USERNAME,
                        requireContext().resources.getString(R.string.enter_new_name),
                        newState.user.name
                    ) {
                        if (it.isNotEmpty()) viewModel.addAction(UpdateNameAction(it))
                    }
                    bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
                }
            }
            if (newState.user.status != currentState.user?.status) {
                Log.i("SettingsFragment","diff status")
                binding.statusText.text =
                    newState.user.status ?: requireContext().resources.getText(R.string.no_status)
                binding.editStatus.setOnClickListener {
                    val bottomSheetFragment = EditUserBottomDialogFragment(
                        SettingType.EDIT_STATUS,
                        requireContext().resources.getString(R.string.enter_new_status),
                        newState.user.status
                    ) {
                        Log.i("SettingsFragment","entered $it isNotEmpty ${it.isNotEmpty()}")
                        if (it.isNotEmpty()) viewModel.addAction(UpdateStatusAction(it))
                    }
                    bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
                }
            }
            if (newState.user.phone != currentState.user?.phone)
                binding.phoneText.text = newState.user.phone
        }
    }

    private fun addListeners() {
        binding.editPhoto.setOnClickListener {
            requestReadPermission()
        }

        binding.notificationsSwitch.isChecked =
            getSetting<Boolean>(requireContext(), NOTIFICATIONS) != null
        binding.soundSwitch.isChecked =
            getSetting<Boolean>(requireContext(), SOUND) != null
        binding.vibrationSwitch.isChecked =
            getSetting<Boolean>(requireContext(), VIBRATION) != null

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            setSetting(requireContext(), NOTIFICATIONS, isChecked)
        }

        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            setSetting(requireContext(), SOUND, isChecked)
        }

        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            setSetting(requireContext(), VIBRATION, isChecked)
        }

        binding.privacyPolicy.setOnClickListener { openPrivacyPolicy() }
    }

    private fun openPrivacyPolicy() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("https://bogomolovaa.github.io/Anochat/")
        startActivity(i)
    }

    private fun updatePhoto(uri: Uri) {
        //val path = getPath(requireContext(), uri)
        val resizedImage = resizeImage(uri, requireContext())
        val bundle = Bundle().apply { putString("image", resizedImage) }
        navController.navigate(R.id.miniatureFragment, bundle)
        //binding.userPhoto.setFile(resizedImage)
        //viewModel.updatePhoto(resizedImage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FILE_CHOOSER_CODE -> {
                    if (intent != null) {
                        val uri = intent.data
                        if (uri != null) updatePhoto(uri)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_PERMISSIONS_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startFileChooser()
                } else {
                    Log.i("test", "read perm not granted")
                }
            }
        }
    }

    private fun requestReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(READ_PERMISSION), READ_PERMISSIONS_CODE)
    }


    companion object {
        private const val FILE_CHOOSER_CODE: Int = 0
        private const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val READ_PERMISSIONS_CODE = 1001
    }


}
