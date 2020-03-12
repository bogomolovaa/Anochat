package bogomolov.aa.anochat.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.UserEditLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditUserBottomDialogFragment(
    private val title: String,
    val onSave: (String) -> Unit
) : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<UserEditLayoutBinding>(
            inflater,
            R.layout.user_edit_layout,
            container,
            false
        )
        binding.enterLabel.text = title
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        binding.saveButton.setOnClickListener {
            val text = binding.enterText.text.toString()
            Log.i("test","save text $text")
            onSave(text)
            dismiss()
        }
        return binding.root
    }


}