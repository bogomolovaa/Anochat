package bogomolov.aa.anochat.repository

import android.util.Log
import bogomolov.aa.anochat.domain.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FirebaseRepository"

class Firebase @Inject constructor() {
    private lateinit var token: String

    init {
        GlobalScope.launch(Dispatchers.IO) {
            token = getToken()
            updateOnlineStatus()
        }
    }

    fun signUp(name: String, email: String, password: String): Boolean {
        return true
    }

    suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): String? {
        val uid = userSignIn(credential) ?: return null
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("user_tokens").child(uid)
            .setValue(mapOf("token" to token))
        myRef.child("users").child(uid)
            .updateChildren(mapOf("phone" to phoneNumber))
        return uid
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    fun isSignedIn() = getUid() != null


    fun addUserStatusListener(
        uid: String,
        scope: CoroutineScope
    ): Flow<Pair<Boolean, Long>> {
        val userRef = FirebaseDatabase.getInstance().getReference("users/${uid}")
        val onlineFlow = MutableSharedFlow<Boolean>()
        val lastTimeFlow = MutableSharedFlow<Long>()
        val onlineListener =
            userRef.child("online").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val online = snapshot.getValue(Int::class.java) ?: false
                    scope.launch(Dispatchers.IO) {
                        onlineFlow.emit(online == 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        val lastOnlineListener =
            userRef.child("lastOnline").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastOnline = snapshot.getValue(Long::class.java) ?: 0L
                    scope.launch(Dispatchers.IO) {
                        lastTimeFlow.emit(lastOnline)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        scope.launch(Dispatchers.IO) {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                userRef.removeEventListener(onlineListener)
                userRef.removeEventListener(lastOnlineListener)
            }
        }
        return onlineFlow.combine(lastTimeFlow) { online, time -> Pair(online, time) }
    }

    suspend fun findByPhone(phone: String): List<User> = suspendCoroutine {
        val users = ArrayList<User>()
        val query = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("phone")
            .equalTo(phone)
        Log.i("test", "findUsers")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                    for (user in snapshot.children) users += userFromRef(user)
                it.resume(users)
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.w(TAG, "Firebase request cancelled: $p0")
                it.resume(listOf())
            }
        })
    }

    suspend fun receiveUsersByPhones(phones: List<String>): List<User> = suspendCoroutine {
        val phonesMap = HashMap<String, String>()
        for (phone in phones) phonesMap[phone] = ""
        val ref = FirebaseDatabase.getInstance().reference.child("requests").push()
        ref.setValue(phonesMap)
        val requestKey = ref.key
        Log.i("test", "respRef.addValueEventListener")
        val respRef = FirebaseDatabase.getInstance().getReference("responses/$requestKey")
        respRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != null) {
                    val users = ArrayList<User>()
                    for (child in snapshot.children) {
                        val user = userFromRef(child)
                        users.add(user)
                    }
                    respRef.removeValue()
                    it.resume(users)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.w(TAG, "Firebase request cancelled: $p0")
                it.resume(listOf())
            }
        })
    }

    fun sendReport(messageId: String, received: Int, viewed: Int) {
        Log.i(TAG, "sendReport messageId $messageId")
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").child(messageId)
            .updateChildren(mapOf("received" to received.toString(), "viewed" to viewed.toString()))
    }

    fun sendMessage(
        text: String? = null,
        replyId: String? = null,
        image: String? = null,
        audio: String? = null,
        uid: String,
        publicKey: String? = null,
        initiator: Boolean = false
    ): String {
        val ref = FirebaseDatabase.getInstance().reference.child("messages").push()
        ref.setValue(
            mapOf(
                "message" to text,
                "reply" to replyId,
                "image" to image,
                "audio" to audio,
                "dest" to uid,
                "source" to token,
                "key" to publicKey,
                "initiator" to if (initiator) 1 else 0
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

    fun updatePhoto(uid: String, photo: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("photo" to photo))
    }

    suspend fun uploadFile(
        fileName: String,
        uid: String,
        byteArray: ByteArray,
        isPrivate: Boolean = false
    ): Boolean =
        suspendCoroutine { continuation ->
            val path = if (isPrivate) "/files/" else "/user/$uid/"
            val fileRef = FirebaseStorage.getInstance().getReference(path).child(fileName)
            Log.i("test", "start uploading $fileName to ${fileRef.path} myUid")

            fileRef.putBytes(byteArray).addOnSuccessListener {
                Log.i("test", "uploaded $fileName")
                continuation.resume(true)
            }.addOnFailureListener {
                Log.i("test", "NOT uploaded $fileName")
                it.printStackTrace()
                continuation.resume(false)
            }
        }

    suspend fun downloadFile(
        fileName: String,
        uid: String,
        localFile: File,
        isPrivate: Boolean = false
    ): Boolean =
        suspendCoroutine { continuation ->
            val path = if (isPrivate) "/files/" else "/user/$uid/"
            val fileRef = FirebaseStorage.getInstance()
                .getReference(path).child(fileName)
            Log.i("test", "start downloading: $fileName ref $fileRef")

            fileRef.getFile(localFile).addOnSuccessListener {
                Log.i("test", "downloaded $fileName")
                if (isPrivate) fileRef.delete()
                continuation.resume(true)
            }.addOnFailureListener {
                Log.i("test", "NOT downloaded $fileName")
                continuation.resume(false)
            }
        }


    fun deleteRemoteMessage(messageId: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").child(messageId).removeValue()
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


    private fun updateOnlineStatus() {
        val uid = getUid()
        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/${uid}")
            userRef.child("online").setValue(1)
            userRef.child("online").onDisconnect().setValue(0)
            userRef.child("lastOnline").onDisconnect().setValue(ServerValue.TIMESTAMP)
        }
    }

    private fun userFromRef(snapshot: DataSnapshot): User {
        val uid = snapshot.key!!
        val name = snapshot.child("name").value.toString()
        val phone = snapshot.child("phone").value.toString()
        val status = snapshot.child("status").value?.toString()
        val photo = snapshot.child("photo").value?.toString()
        return User(uid = uid, phone = phone, name = name, status = status, photo = photo)
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