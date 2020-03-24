package bogomolov.aa.anochat.view.fragments

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.*
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentSettingsBinding
import bogomolov.aa.anochat.viewmodel.SettingsViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class SettingsFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: SettingsViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var navController: NavController

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_settings,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        val uid = getSetting<String>(requireContext(), UID)
        viewModel.loadUser(uid!!)

        binding.editPhoto.setOnClickListener {
            requestReadPermission()
        }

        binding.editUsername.setOnClickListener {
            val bottomSheetFragment = EditUserBottomDialogFragment(
                requireContext().resources.getString(R.string.enter_new_name)
            ) {
                if (it.isNotEmpty()) viewModel.updateName(it)
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

        binding.editStatus.setOnClickListener {
            val bottomSheetFragment = EditUserBottomDialogFragment(
                requireContext().resources.getString(R.string.enter_new_status)
            ) {
                if (it.isNotEmpty()) viewModel.updateStatus(it)
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
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

        return binding.root
    }

    private fun updatePhoto(uri: Uri) {
        val path = getPath(requireContext(), uri)
        val resizedImage = resizeImage(path, requireContext())
        val bundle = Bundle().apply { putString("image", resizedImage) }
        navController.navigate(R.id.miniatureFragment, bundle)
        //binding.userPhoto.setFile(resizedImage)
        //viewModel.updatePhoto(resizedImage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Log.i("test", "onActivityResult $resultCode $intent requestCode $requestCode")
        if (resultCode == Activity.RESULT_OK) {
            var uri: Uri? = null
            when (requestCode) {
                FILE_CHOOSER_CODE -> {
                    if (intent != null) {
                        uri = intent.data
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
