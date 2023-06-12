package bogomolov.aa.anochat.features.shared

import android.app.Activity
import bogomolov.aa.anochat.domain.MessageUseCases

data class SignInError(val message: String) {
    constructor(errorType: ErrorType) : this(errorType.toString())
}

enum class ErrorType {
    WRONG_CODE,
    EMPTY_CODE,
    WRONG_PHONE,
    PHONE_NO_CONNECTION,
    CODE_NO_CONNECTION
}

interface PhoneVerification {
    fun onComplete()
    fun onCodeVerify(smsCode: String?)
    fun onCodeSent()
    fun onPhoneError(error: SignInError?)
    fun onCodeError(error: SignInError?)
}

interface AuthRepository {

    fun initAuthListener(messageUseCases: MessageUseCases)

    suspend fun sendPhoneNumber(
        phoneNumber: String,
        getActivity: () -> Activity,
        phoneVerification: PhoneVerification,
    )

    suspend fun verifySmsCode(
        phoneNumber: String,
        code: String,
        phoneVerification: PhoneVerification
    )

    suspend fun signOut()
    fun isSignedIn(): Boolean

    suspend fun updateSettings(settings: Settings)
    suspend fun getSettings(): Settings
}