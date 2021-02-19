package bogomolov.aa.anochat.features.settings

import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.UserEditLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

enum class SettingType { EDIT_USERNAME, EDIT_STATUS }

class EditUserBottomDialogFragment(
    private val settingType: SettingType,
    private val title: String,
    private val currentValue: String?,
    val onSave: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = UserEditLayoutBinding.inflate(inflater, container, false)
        binding.enterLabel.text = title
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.saveButton.setOnClickListener {
            onSave(binding.enterText.text.toString())
            dismiss()
        }
        var maxLength = 0
        if (settingType == SettingType.EDIT_USERNAME) maxLength = 20
        if (settingType == SettingType.EDIT_STATUS) maxLength = 40
        binding.enterText.filters = arrayOf<InputFilter>(LengthFilter(maxLength))
        if (!currentValue.isNullOrEmpty()) binding.enterText.setText(currentValue)
        return binding.root
    }
}