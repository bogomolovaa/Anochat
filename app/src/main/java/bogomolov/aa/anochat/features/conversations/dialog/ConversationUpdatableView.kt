package bogomolov.aa.anochat.features.conversations.dialog

import android.view.View
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.features.shared.getBitmap
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView

class ConversationUpdatableView(
    private val fragment: ConversationFragment,
    private val recyclerViewSetup: ConversationRecyclerViewSetup
) : UpdatableView<DialogUiState> {
    lateinit var binding: FragmentConversationBinding
    private var lastMessageAudioView: PlayAudioView? = null
    private var lastReplyMessageAudioView: PlayAudioView? = null

    override fun updateView(newState: DialogUiState, currentState: DialogUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData)
            recyclerViewSetup.setPagedListLiveData(newState.pagedListLiveData!!)
        if (newState.conversation != currentState.conversation) setConversation(newState.conversation!!)
        if (newState.onlineStatus != currentState.onlineStatus)
            binding.statusText.text = newState.onlineStatus
        if (newState.audioLengthText != currentState.audioLengthText)
            binding.audioLengthText.text = newState.audioLengthText
        if (newState.replyMessage != currentState.replyMessage) setReplyMessage(newState.replyMessage)
        if (newState.inputState != currentState.inputState) setInputState(newState)
        if (newState.playingState != currentState.playingState) setPlayingState(newState)
    }

    private fun setPlayingState(state: DialogUiState) {
        val playingState = state.playingState
        if (playingState != null) {
            val messageId = playingState.messageId
            if (messageId != null) {
                val messageAudioView = recyclerViewSetup.getMessageAudioView(messageId)
                messageAudioView?.setPlayingState(playingState)
                lastMessageAudioView = messageAudioView
                val replyMessageAudioView = recyclerViewSetup.getReplyMessageAudioView(messageId)
                replyMessageAudioView?.setPlayingState(playingState)
                lastReplyMessageAudioView = replyMessageAudioView

                if (state.replyMessage?.messageId == messageId)
                    binding.replayAudio.setPlayingState(playingState)
            } else {
                val inputAudioView = binding.playAudioInput
                inputAudioView.setPlayingState(playingState)
            }
        } else {
            lastMessageAudioView?.setInitialState()
            lastReplyMessageAudioView?.setInitialState()
            binding.playAudioInput.setInitialState()
            binding.replayAudio.setInitialState()
        }
    }


    private fun setReplyMessage(replyMessage: Message?) {
        if (replyMessage == null) {
            binding.replyLayout.visibility = View.GONE
            binding.replyImage.visibility = View.INVISIBLE
            binding.replayAudio.visibility = View.INVISIBLE
        } else {
            binding.replyLayout.visibility = View.VISIBLE
            binding.replyText.text = replyMessage.text
            if (replyMessage.image != null) {
                val bitmap = getBitmap(replyMessage.image, binding.root.context, 4)
                if (bitmap != null) {
                    binding.replyImage.setImageBitmap(bitmap)
                    binding.replyImage.visibility = View.VISIBLE
                }
            }
            if (replyMessage.audio != null) {
                binding.replayAudio.set(replyMessage.audio, replyMessage.messageId)
                binding.replayAudio.visibility = View.VISIBLE
            }
        }
    }

    private fun setInputState(state: DialogUiState) {
        hideInput(state)
        if (state.inputState == InputStates.INITIAL) {
            binding.textLayout.visibility = View.VISIBLE
            binding.messageInputText.setText("")
            binding.fab.setImageResource(R.drawable.plus_icon)
        }
        if (state.inputState == InputStates.TEXT_ENTERED) {
            if (binding.messageInputText.text.toString() != state.text)
                binding.messageInputText.setText(state.text)
            binding.fab.setImageResource(R.drawable.send_icon)
        }
        if (state.inputState == InputStates.FAB_EXPAND) {
            binding.textLayout.visibility = View.VISIBLE
            binding.fabFile.visibility = View.VISIBLE
            binding.fabMic.visibility = View.VISIBLE
            binding.fabCamera.visibility = View.VISIBLE
            binding.fab.setImageResource(R.drawable.clear_icon)
        }
        if (state.inputState == InputStates.VOICE_RECORDING) {
            binding.audioLayout.visibility = View.VISIBLE
            binding.fab.setImageResource(R.drawable.stop_icon)
        }
        if (state.inputState == InputStates.VOICE_RECORDED) {
            binding.playAudioInput.visibility = View.VISIBLE
            if (state.audioFile != null) binding.playAudioInput.set(state.audioFile)
            binding.fab.setImageResource(R.drawable.send_icon)
        }
    }

    private fun hideInput(state: DialogUiState) {
        //fab
        binding.fabFile.visibility = View.INVISIBLE
        binding.fabMic.visibility = View.INVISIBLE
        binding.fabCamera.visibility = View.INVISIBLE
        //audio
        binding.playAudioInput.visibility = View.GONE
        binding.audioLayout.visibility = View.GONE
        //text
        if (state.inputState != InputStates.TEXT_ENTERED) binding.textLayout.visibility = View.GONE
    }

    private fun setConversation(conversation: Conversation) {
        if (conversation.user.photo != null) binding.userPhoto.setFile(conversation.user.photo)
        binding.usernameText.text = conversation.user.name
        binding.usernameLayout.setOnClickListener { fragment.navigateToUserFragment(conversation.user.id) }
    }
}