package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.KeyValueStore
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

    override fun updateSettings(settings: Settings) {
        keyValueStore.setValue(Settings.NOTIFICATIONS, settings.notifications)
        keyValueStore.setValue(Settings.SOUND, settings.sound)
        keyValueStore.setValue(Settings.VIBRATION, settings.vibration)
        keyValueStore.setValue(Settings.GALLERY, settings.gallery)
    }

    override fun getSettings() = Settings(
        notifications = keyValueStore.getBooleanValue(Settings.NOTIFICATIONS),
        sound = keyValueStore.getBooleanValue(Settings.SOUND),
        vibration = keyValueStore.getBooleanValue(Settings.VIBRATION),
        gallery = keyValueStore.getBooleanValue(Settings.GALLERY)
    )

}