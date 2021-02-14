package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.repository.Firebase
import com.google.firebase.auth.PhoneAuthCredential
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    fun signUp(name: String, email: String, password: String): Boolean
    suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): Boolean
    fun signOut()
    fun isSignedIn(): Boolean
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebase: Firebase
) : AuthRepository by firebase