package bogomolov.aa.anochat.features.conversations.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.animation.DecelerateInterpolator
import androidx.core.widget.doOnTextChanged
import bogomolov.aa.anochat.features.shared.playMessageSound

class ConversationInputSetup(
    private val fragment: ConversationFragment,
    private val viewModel: ConversationViewModel
) {
    private val binding get() = fragment.binding
    private val recyclerViewSetup get() = fragment.recyclerViewSetup!!

    fun setup() {
        setFabClickListener()
        setMiniFabsClickListeners()
        setMessageInputTextListeners()
        binding.playAudioInput.setOnCloseListener {
            viewModel.updateState { copy(inputState = InputStates.INITIAL, audioFile = null) }
        }
        binding.playAudioInput.actionExecutor = viewModel
        binding.replayAudio.actionExecutor = viewModel
    }

    private fun setFabClickListener() {
        binding.fab.setOnClickListener {
            when (viewModel.currentState.inputState) {
                InputStates.INITIAL -> expandFabs()
                InputStates.FAB_EXPAND -> hideFabs {
                    viewModel.updateState { copy(inputState = InputStates.INITIAL) }
                }
                InputStates.TEXT_ENTERED -> {
                    viewModel.sendMessage(SendMessageData(text = viewModel.currentState.text))
                    playMessageSound(binding.root.context)
                }
                InputStates.VOICE_RECORDED -> {
                    viewModel.sendMessage(SendMessageData(audio = viewModel.currentState.audioFile))
                    playMessageSound(binding.root.context)
                }
                InputStates.VOICE_RECORDING -> viewModel.stopRecording()
            }
        }
    }

    private fun setMiniFabsClickListeners() {
        binding.fabMic.setOnClickListener {
            hideFabs()
            fragment.requestMicrophonePermission()
        }
        binding.fabFile.setOnClickListener {
            hideFabs { viewModel.updateState { copy(inputState = InputStates.INITIAL) } }
            fragment.requestReadPermission()
        }
        binding.fabCamera.setOnClickListener {
            hideFabs { viewModel.updateState { copy(inputState = InputStates.INITIAL) } }
            fragment.requestCameraPermission()
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
        viewModel.updateState { copy(inputState = InputStates.FAB_EXPAND) }
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
            viewModel.textChanged(enteredText)
        }
    }
}