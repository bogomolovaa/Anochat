package bogomolov.aa.anochat.features.login

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.main.LocalNavController
import bogomolov.aa.anochat.features.main.Route
import bogomolov.aa.anochat.features.main.theme.MyTopAppBar
import bogomolov.aa.anochat.features.shared.*

@Composable
fun SignInView(getActivity: (() -> Activity)?) {
    val navController = LocalNavController.current
    val viewModel = hiltViewModel<SignInViewModel>()
    viewModel.events.collectEvents {
        if (it is NavigateToConversationList) navController?.navigate(Route.Conversations.route)
    }
    viewModel.state.collectState { Content(it, viewModel, getActivity) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Content(
    state: SignInUiState = testSignInUiState,
    viewModel: SignInViewModel? = null,
    getActivity: (() -> Activity)? = null
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            MyTopAppBar(
                title = { Text(stringResource(id = R.string.sign_in)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = remember { { fabOnClick(viewModel, getActivity) } }) {
                if (state.state == LoginState.INITIAL || state.state == LoginState.VERIFICATION_ID_RECEIVED || state.state == LoginState.NOT_LOGGED)
                    Icon(
                        painterResource(id = R.drawable.ok_icon),
                        contentDescription = "",
                        modifier = Modifier.scale(1.5f)
                    )
            }
        }) { padding ->
        Box(
            modifier = createInsetsModifier(padding)
                .fillMaxWidth()
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
                val phoneErrorMessage = phoneInputErrorMessage(state, context)
                TextField(
                    value = state.phoneNumber ?: "",
                    onValueChange = remember { { viewModel?.setPhone(it) } },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    singleLine = true,
                    label = {
                        if (phoneErrorMessage != null) {
                            Text(phoneErrorMessage)
                        } else {
                            Text(
                                text = stringResource(id = R.string.phone_number),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    isError = phoneErrorMessage != null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                if (state.state.ordinal >= LoginState.VERIFICATION_ID_RECEIVED.ordinal) {
                    val codeErrorMessage = codeInputErrorMessage(state, context)
                    TextField(
                        value = state.code ?: "",
                        onValueChange = remember { { viewModel?.setCode(it) } },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        label = {
                            if (codeErrorMessage != null) {
                                Text(codeErrorMessage)
                            } else {
                                Text(
                                    stringResource(id = R.string.verification_code),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        isError = codeErrorMessage != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    SideEffect {
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }
}

private fun phoneInputErrorMessage(state: SignInUiState, context: Context) =
    state.error?.let {
        when (it.message) {
            ErrorType.WRONG_PHONE.toString() -> context.getString(R.string.wrong_phone)
            ErrorType.PHONE_NO_CONNECTION.toString() -> context.getString(R.string.no_connection)
            else -> if (state.state == LoginState.INITIAL || state.state == LoginState.PHONE_SUBMITTED) {
                it.message
            } else null
        }
    }


private fun codeInputErrorMessage(state: SignInUiState, context: Context) =
    state.error?.let {
        when (it.message) {
            ErrorType.EMPTY_CODE.toString() -> context.getString(R.string.empty_code)
            ErrorType.WRONG_CODE.toString() -> context.getString(R.string.wrong_code)
            ErrorType.CODE_NO_CONNECTION.toString() -> context.getString(R.string.no_connection)
            else -> if (state.state == LoginState.INITIAL || state.state == LoginState.PHONE_SUBMITTED) {
                null
            } else it.message
        }
    }

private fun fabOnClick(viewModel: SignInViewModel?, getActivity: (() -> Activity)?) {
    when (viewModel?.currentState?.state) {
        LoginState.INITIAL -> {
            getActivity?.let { viewModel.submitPhoneNumber(it) }
        }
        LoginState.VERIFICATION_ID_RECEIVED, LoginState.NOT_LOGGED ->
            viewModel.submitSmsCode()
        else -> {
        }
    }
}