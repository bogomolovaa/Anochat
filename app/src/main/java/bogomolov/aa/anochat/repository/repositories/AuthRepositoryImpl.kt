package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.getValue
import bogomolov.aa.anochat.domain.setMyUID
import bogomolov.aa.anochat.domain.setValue
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.Settings
import bogomolov.aa.anochat.repository.Firebase
import com.google.firebase.auth.PhoneAuthCredential
import javax.inject.Inject
import javax.inject.Singleton

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

    companion object {
        private const val NOTIFICATIONS = "notifications"
        private const val SOUND = "sound"
        private const val VIBRATION = "vibration"
    }

    override fun updateSettings(settings: Settings) {
        keyValueStore.setValue(NOTIFICATIONS, settings.notifications)
        keyValueStore.setValue(SOUND, settings.sound)
        keyValueStore.setValue(VIBRATION, settings.vibration)
    }

    override fun getSettings() = Settings(
        notifications = keyValueStore.getValue(NOTIFICATIONS) ?: true,
        sound = keyValueStore.getValue(SOUND) ?: true,
        vibration = keyValueStore.getValue(VIBRATION) ?: true
    )

}