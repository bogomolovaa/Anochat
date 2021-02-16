package bogomolov.aa.anochat.features.shared

import bogomolov.aa.anochat.features.shared.Settings
import com.google.firebase.auth.PhoneAuthCredential

interface AuthRepository {
    fun signUp(name: String, email: String, password: String): Boolean
    suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): Boolean
    fun signOut()
    fun isSignedIn(): Boolean

    fun updateSettings(settings: Settings)
    fun getSettings(): Settings
}