package bogomolov.aa.anochat.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.shared.ErrorType
import bogomolov.aa.anochat.features.shared.LightColorPalette
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class SignInFragment : Fragment() {
    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent {
                val state = viewModel.state.collectAsState()
                Content(state.value)
            }
        }

    @Preview
    @Composable
    private fun Content(state: SignInUiState = testSignInUiState) {
        MaterialTheme(
            colors = LightColorPalette
        ) {
            val focusRequester = remember { FocusRequester() }
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.sign_in)) }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { fabOnClick() }) {
                        if (state.state == LoginState.INITIAL || state.state == LoginState.VERIFICATION_ID_RECEIVED || state.state == LoginState.NOT_LOGGED)
                            Icon(
                                painterResource(id = R.drawable.ok_icon),
                                contentDescription = "",
                                modifier = Modifier.scale(1.5f)
                            )
                    }
                },
                content = {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.state == LoginState.PHONE_SUBMITTED || state.state == LoginState.CODE_SUBMITTED)
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                            )
                        Column(
                            modifier = Modifier.padding(top = 32.dp)
                        ) {
                            val phoneErrorMessage = phoneInputErrorMessage(state)
                            TextField(
                                value = state.phoneNumber ?: "",
                                onValueChange = { viewModel.updateState { copy(phoneNumber = it) } },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                label = {
                                    if (phoneErrorMessage != null) {
                                        Text(phoneErrorMessage)
                                    } else {
                                        Text(
                                            stringResource(id = R.string.phone_number),
                                            color = LightColorPalette.secondary
                                        )
                                    }
                                },
                                isError = phoneErrorMessage != null,
                                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                            )
                            if (state.state.ordinal >= LoginState.VERIFICATION_ID_RECEIVED.ordinal) {
                                val codeErrorMessage = codeInputErrorMessage(state)
                                TextField(
                                    value = state.code ?: "",
                                    onValueChange = { viewModel.updateState { copy(code = it) } },
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    label = {
                                        if (codeErrorMessage != null) {
                                            Text(codeErrorMessage)
                                        } else {
                                            Text(
                                                stringResource(id = R.string.verification_code),
                                                color = LightColorPalette.secondary
                                            )
                                        }
                                    },
                                    isError = codeErrorMessage != null,
                                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                                )
                                SideEffect {
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    private fun fabOnClick() {
        when (viewModel.currentState.state) {
            LoginState.INITIAL -> {
                viewModel.submitPhoneNumber { requireActivity() }
            }
            LoginState.VERIFICATION_ID_RECEIVED, LoginState.NOT_LOGGED ->
                viewModel.submitSmsCode()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.events.collect {
                when (it) {
                    is NavigateToConversationList -> findNavController().navigate(R.id.conversationsListFragment)
                }
            }
        }
    }

    @Composable
    private fun phoneInputErrorMessage(state: SignInUiState) =
        state.error?.let {
            when (it.message) {
                ErrorType.WRONG_PHONE.toString() -> stringResource(R.string.wrong_phone)
                ErrorType.PHONE_NO_CONNECTION.toString() -> stringResource(R.string.no_connection)
                else -> if (state.state == LoginState.INITIAL || state.state == LoginState.PHONE_SUBMITTED) {
                    it.message
                } else null
            }
        }


    @Composable
    private fun codeInputErrorMessage(state: SignInUiState) =
        state.error?.let {
            when (it.message) {
                ErrorType.EMPTY_CODE.toString() -> stringResource(R.string.empty_code)
                ErrorType.WRONG_CODE.toString() -> stringResource(R.string.wrong_code)
                ErrorType.CODE_NO_CONNECTION.toString() -> stringResource(R.string.no_connection)
                else -> if (state.state == LoginState.INITIAL || state.state == LoginState.PHONE_SUBMITTED) {
                    null
                } else it.message
            }
        }
}