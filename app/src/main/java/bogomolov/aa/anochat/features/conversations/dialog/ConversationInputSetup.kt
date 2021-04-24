package bogomolov.aa.anochat.features.conversations.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.core.widget.doOnTextChanged
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.features.shared.playMessageSound

interface RequestPermission {
    fun requestMicrophonePermission()
    fun requestReadPermission()
    fun requestCameraPermission()
}

class ConversationInputSetup(
    private val requestPermission: RequestPermission,
    private val viewModel: ConversationViewModel,
    private val recyclerViewSetup: ConversationRecyclerViewSetup
) {
    private lateinit var binding: FragmentConversationBinding

    fun setup(binding: FragmentConversationBinding) {
        this.binding = binding
        setFabClickListener()
        setMiniFabsClickListeners()
        setMessageInputTextListeners()
        binding.playAudioInput.setOnCloseListener {
            viewModel.setStateAsync { copy(inputState = InputStates.INITIAL, audioFile = null) }
        }
        binding.playAudioInput.actionExecutor = viewModel
        binding.replayAudio.actionExecutor = viewModel
    }

    private fun setFabClickListener() {
        binding.fab.setOnClickListener {
            when (viewModel.state.inputState) {
                InputStates.INITIAL -> expandFabs()
                InputStates.FAB_EXPAND -> hideFabs {
                    viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) }
                }
                InputStates.TEXT_ENTERED -> {
                    viewModel.addAction(SendMessageAction(text = viewModel.state.text))
                    playMessageSound(binding.root.context)
                }
                InputStates.VOICE_RECORDED -> {
                    viewModel.addAction(SendMessageAction(audio = viewModel.state.audioFile))
                    playMessageSound(binding.root.context)
                }
                InputStates.VOICE_RECORDING -> viewModel.addAction(StopRecordingAction())
            }
        }
    }

    private fun setMiniFabsClickListeners() {
        binding.fabMic.setOnClickListener {
            hideFabs()
            requestPermission.requestMicrophonePermission()
        }
        binding.fabFile.setOnClickListener {
            hideFabs { viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) } }
            requestPermission.requestReadPermission()
        }
        binding.fabCamera.setOnClickListener {
            hideFabs { viewModel.setStateAsync { copy(inputState = InputStates.INITIAL) } }
            requestPermission.requestCameraPermission()
        }
    }

    private fun hideFabs(onAnimationEnd: () -> Unit = {}) {
        binding.fabMic.animate()
            .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(var1: Animator) {
                    binding.fabMic.animate().setListener(null)
                    onAnimationEnd()
                }
            }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.animate()
            .translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.animate()
            .translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun expandFabs() {
        viewModel.setStateAsync { copy(inputState = InputStates.FAB_EXPAND) }
        binding.fabMic.animate()
            .translationY(-600f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabFile.animate()
            .translationY(-200f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        binding.fabCamera.animate()
            .translationY(-400f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun setMessageInputTextListeners() {
        binding.messageInputText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) recyclerViewSetup.scrollToEnd()
        }
        binding.messageInputText.doOnTextChanged { textInput, _, _, _ ->
            val enteredText = textInput.toString()
            viewModel.addAction(TextChangedAction(enteredText))
        }
    }
}