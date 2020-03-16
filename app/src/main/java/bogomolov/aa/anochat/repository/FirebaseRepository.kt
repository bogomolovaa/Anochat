package bogomolov.aa.anochat.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.TOKEN
import bogomolov.aa.anochat.android.UID
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.android.setSetting
import bogomolov.aa.anochat.core.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


interface IFirebaseRepository {
    suspend fun findUsers(startWith: String): List<User>
    suspend fun signUp(name: String, email: String, password: String): Boolean
    suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): Boolean
    fun signOut()
    fun isSignedIn(): Boolean
    suspend fun uploadFile(fileName: String, uid: String? = null): Boolean
    suspend fun downloadFile(fileName: String, uid: String? = null): Boolean
    suspend fun sendReport(messageId: String, received: Int, viewed: Int)
    suspend fun deleteRemoteMessage(messageId: String)
    suspend fun addUserStatusListener(
        uid: String,
        isOnline: (Boolean) -> Unit,
        lastTimeOnline: (Long) -> Unit
    ): () -> Unit
}

class FirebaseRepository @Inject constructor(val context: Context) : IFirebaseRepository {
    private lateinit var token: String
    //TODO: turn on caching

    init {
        //signOut()
        GlobalScope.launch(Dispatchers.IO) {
            token = getToken()
            updateOnlineStatus()
        }
    }

    override suspend fun addUserStatusListener(
        uid: String,
        isOnline: (Boolean) -> Unit,
        lastTimeOnline: (Long) -> Unit
    ): () -> Unit {
        val userRef = FirebaseDatabase.getInstance().getReference("users/${uid}")

        val onlineListener =
            userRef.child("online").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val online = snapshot.getValue(Int::class.java) ?: false
                    isOnline(online == 1)
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        val lastOnlineListener =
            userRef.child("lastOnline").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastOnline = snapshot.getValue(Long::class.java) ?: 0L
                    lastTimeOnline(lastOnline)
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        return {
            userRef.removeEventListener(onlineListener)
            userRef.removeEventListener(lastOnlineListener)
        }
    }

    private fun updateOnlineStatus() {
        val uid = getUid()
        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/${uid}")
            userRef.child("lastOnline").onDisconnect().setValue(ServerValue.TIMESTAMP)
            userRef.child("online").setValue(1)
            userRef.child("online").onDisconnect().setValue(0)
            val connectedRef = database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected)
                        userRef.child("online").onDisconnect().setValue(1)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.i("test", ".info/connected canceled")
                }
            })
        }
    }

    override suspend fun deleteRemoteMessage(messageId: String) {
        Log.i("test", "deleteRemoteMessage messageId $messageId")
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").child(messageId).removeValue()
    }

    override suspend fun sendReport(messageId: String, received: Int, viewed: Int) {
        Log.i("test", "sendReport messageId $messageId")
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").child(messageId)
            .updateChildren(mapOf("received" to received.toString(), "viewed" to viewed.toString()))
    }

    override suspend fun downloadFile(fileName: String, uid: String?): Boolean =
        suspendCoroutine { continuation ->
            Log.i("test", "start downloading: $fileName")
            val fileRef = FirebaseStorage.getInstance()
                .getReference(if (uid != null) "/user/{$uid}/" else "/files/").child(fileName)
            val localFile = File(getFilesDir(context), fileName)
            fileRef.getFile(localFile).addOnSuccessListener {
                Log.i("test", "downloaded $fileName")
                fileRef.delete()
                continuation.resume(true)
            }.addOnFailureListener {
                Log.i("test", "NOT downloaded $fileName")
                continuation.resume(false)
            }
        }

    override suspend fun uploadFile(fileName: String, uid: String?): Boolean =
        suspendCoroutine { continuation ->
            Log.i("test", "start uploading $fileName")
            val fileRef = FirebaseStorage.getInstance()
                .getReference(if (uid != null) "/user/{$uid}/" else "/files/").child(fileName)
            val localFile = File(getFilesDir(context), fileName)
            fileRef.putFile(Uri.fromFile(localFile)).addOnSuccessListener {
                Log.i("test", "uploaded $fileName")
                continuation.resume(true)
            }.addOnFailureListener {
                continuation.resume(false)
                Log.i("test", "NOT uploaded $fileName")
                it.printStackTrace()
            }
        }


    suspend fun getUser(uid: String): User? = suspendCoroutine {
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                it.resume(userFromRef(snapshot))
            }

            override fun onCancelled(p0: DatabaseError) {
                it.resume(null)
            }
        })
    }

    private fun userFromRef(snapshot: DataSnapshot): User {
        val uid = snapshot.key!!
        val name = snapshot.child("name").value.toString()
        val phone = snapshot.child("phone").value.toString()
        val status = snapshot.child("status").value?.toString()
        val photo = snapshot.child("photo").value?.toString()
        return User(uid = uid, phone = phone, name = name, status = status, photo = photo)
    }

    override suspend fun findUsers(startWith: String): List<User> = suspendCoroutine {
        val users = ArrayList<User>()
        val query = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("name")
            .startAt(startWith)
        Log.i("test", "findUsers")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("test", "snapshot $snapshot")

                if (snapshot.exists())
                    for (user in snapshot.children) users += userFromRef(user)
                it.resume(users)
            }

            override fun onCancelled(p0: DatabaseError) {
                it.resumeWithException(Exception("DatabaseError $p0"))
            }
        })
    }

    fun sendMessage(
        text: String?,
        replyId: String?,
        image: String?,
        audio: String?,
        uid: String
    ): String {
        val ref = FirebaseDatabase.getInstance().reference.child("messages").push()
        ref.setValue(
            mapOf(
                "message" to text,
                "reply" to replyId,
                "image" to image,
                "audio" to audio,
                "dest" to uid,
                "source" to token
            )
        )
        return ref.key!!
    }

    fun renameUser(uid: String, name: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("name" to name))
    }

    fun updateStatus(uid: String, status: String?) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("status" to status))
    }

    suspend fun updatePhoto(uid: String, photo: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("photo" to photo))
        uploadFile(photo, uid)
    }

    override suspend fun signUp(name: String, email: String, password: String): Boolean {
        return true
    }

    override suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): Boolean {
        val uid = userSignIn(credential)
        if (uid == null) return false
        saveUidAndToken(uid)
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("user_tokens").child(uid)
            .setValue(mapOf("token" to token))
        myRef.child("users").child(uid)
            .updateChildren(mapOf("phone" to phoneNumber))
        return true
    }

    override fun signOut() {
        FirebaseAuth.getInstance().signOut()
        setSetting(context, UID, "")
    }

    override fun isSignedIn() = getUid() != null


    private fun saveUidAndToken(uid: String) {
        Log.i("test","saveUidAndToken $uid")
        setSetting(context, UID, uid)
        setSetting(context, TOKEN, token)
    }



    private fun getUid() = FirebaseAuth.getInstance().currentUser?.uid

    private suspend fun userSignIn(credential: PhoneAuthCredential): String? = suspendCoroutine {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.i(
                        "test",
                        "Authentication succeeded name: phone: ${user!!.phoneNumber} uid: ${user.uid}"
                    )
                    it.resume(user.uid)
                } else {
                    it.resume(null)
                    Log.i("test", "Authentication failed ")
                }
            }
    }

    private suspend fun getToken(): String = suspendCoroutine {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful)
                    it.resumeWithException(Exception("getInstanceId failed"))
                val token = task.result!!.token
                Log.d("test", "token $token")
                it.resume(token)
            }
    }

}