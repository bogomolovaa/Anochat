package bogomolov.aa.anochat.features.shared

import android.app.Activity

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
    fun onPhoneError(error: ErrorType?)
    fun onCodeError(error: ErrorType?)
}

interface AuthRepository {
    fun sendPhoneNumber(
        phoneNumber: String,
        activity: () -> Activity,
        phoneVerification: PhoneVerification,
    )

    suspend fun verifySmsCode(
        phoneNumber: String,
        code: String,
        phoneVerification: PhoneVerification
    )

    fun signOut()
    fun isSignedIn(): Boolean

    fun updateSettings(settings: Settings)
    fun getSettings(): Settings
}