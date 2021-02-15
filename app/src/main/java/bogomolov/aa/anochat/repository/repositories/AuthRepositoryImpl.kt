package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.setMyUID
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
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore
) : AuthRepository {

    override suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): Boolean {
        val myUid = firebase.signIn(phoneNumber, credential)
        return if (myUid != null) {
            keyValueStore.setMyUID(myUid)
            true
        } else false
    }

    override fun signOut() {
        firebase.signOut()
        keyValueStore.setMyUID(null)
    }

    override fun signUp(name: String, email: String, password: String) =
        firebase.signUp(name, email, password)

    override fun isSignedIn() = firebase.isSignedIn()

}