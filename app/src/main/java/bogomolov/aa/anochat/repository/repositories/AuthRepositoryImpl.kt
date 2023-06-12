package bogomolov.aa.anochat.repository.repositories

import android.app.Activity
import android.util.Log
import bogomolov.aa.anochat.domain.*
import bogomolov.aa.anochat.features.shared.*
import bogomolov.aa.anochat.repository.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AuthRepositoryImpl"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val keyValueStore: KeyValueStore,
    private val firebase: Firebase,
    private val dispatcher: CoroutineDispatcher
) : AuthRepository {
    private var phoneVerificationId: String? = null

    override suspend fun signOut() {
        withContext(dispatcher) {
            firebase.setOffline()
            FirebaseAuth.getInstance().signOut()
        }
    }

    override fun initAuthListener(messageUseCases: MessageUseCases) {
        FirebaseAuth.getInstance().addAuthStateListener {
            if (it.currentUser?.uid != null) {
                firebase.setMessagesListener(messageUseCases.messagesListener)
            } else {
                firebase.removeMessagesListener()
            }
        }
    }

    override fun isSignedIn() = FirebaseAuth.getInstance().currentUser?.uid != null

    override suspend fun updateSettings(settings: Settings) {
        withContext(dispatcher) {
            keyValueStore.setValue(Settings.NOTIFICATIONS, settings.notifications)
            keyValueStore.setValue(Settings.SOUND, settings.sound)
            keyValueStore.setValue(Settings.VIBRATION, settings.vibration)
            keyValueStore.setValue(Settings.GALLERY, settings.gallery)
        }
    }

    override suspend fun getSettings() =
        withContext(dispatcher) {
            Settings(
                notifications = keyValueStore.getBooleanValue(Settings.NOTIFICATIONS) ?: true,
                sound = keyValueStore.getBooleanValue(Settings.SOUND) ?: true,
                vibration = keyValueStore.getBooleanValue(Settings.VIBRATION) ?: true,
                gallery = keyValueStore.getBooleanValue(Settings.GALLERY) ?: true
            )
        }

    override suspend fun verifySmsCode(
        phoneNumber: String,
        code: String,
        phoneVerification: PhoneVerification
    ) {
        withContext(dispatcher) {
            val credential = PhoneAuthProvider.getCredential(phoneVerificationId!!, code)
            signIn(phoneNumber, credential, phoneVerification)
        }
    }

    override suspend fun sendPhoneNumber(
        phoneNumber: String,
        getActivity: () -> Activity,
        phoneVerification: PhoneVerification,
    ) = coroutineScope {
        withContext(dispatcher) {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    launch {
                        signIn(phoneNumber, credential, phoneVerification)
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    phoneVerificationId = verificationId
                    phoneVerification.onCodeSent()
                }

                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                //FirebaseAuthInvalidCredentialsException - Invalid request
                //FirebaseTooManyRequestsException - The SMS quota for the project has been exceeded
                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w(TAG, "onVerificationFailed", e)
                    val errorMessage = when (e) {
                        is FirebaseNetworkException -> ErrorType.PHONE_NO_CONNECTION.toString()
                        is FirebaseAuthInvalidCredentialsException -> ErrorType.WRONG_PHONE.toString()
                        else -> e.message
                    } ?: "empty message"
                    phoneVerification.onPhoneError(SignInError(errorMessage))
                }
            }
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                getActivity(),
                callbacks
            )
        }
    }

    private suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): String? {
        val uid = userSignIn(credential) ?: return null
        if (uid != keyValueStore.getMyUID()) FirebaseMessaging.getInstance().deleteToken()
        val token = firebase.updateToken() ?: return null
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("user_tokens").child(uid)
            .setValue(mapOf("token" to token))
        myRef.child("users").child(uid)
            .updateChildren(mapOf("phone" to phoneNumber))
        return uid
    }

    private suspend fun signIn(
        phoneNumber: String,
        credential: PhoneAuthCredential,
        phoneVerification: PhoneVerification
    ) {
        try {
            phoneVerification.onCodeVerify(credential.smsCode)
            val myUid = signIn(phoneNumber, credential)
            if (myUid != null) keyValueStore.setMyUID(myUid)
            firebase.setOnline()
            phoneVerification.onComplete()
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthInvalidCredentialsException -> ErrorType.WRONG_CODE.toString()
                is FirebaseNetworkException -> ErrorType.CODE_NO_CONNECTION.toString()
                else -> e.message
            } ?: "empty message"
            phoneVerification.onCodeError(SignInError(errorMessage))
        }
    }

    private suspend fun userSignIn(credential: PhoneAuthCredential): String? =
        suspendCancellableCoroutine {
            val auth = FirebaseAuth.getInstance()
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        it.resume(auth.currentUser?.uid)
                    } else {
                        it.resumeWithException(task.exception!!)
                    }
                }
        }

}