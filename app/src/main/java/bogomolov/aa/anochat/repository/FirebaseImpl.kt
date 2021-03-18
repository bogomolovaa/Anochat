package bogomolov.aa.anochat.repository

import android.util.Log
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FirebaseRepository"

class FirebaseImpl : Firebase {
    private lateinit var token: String

    init {
        GlobalScope.launch(Dispatchers.IO) {
            //FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            token = getToken()!!
        }
    }

    override fun addUserStatusListener(
        myUid: String,
        uid: String,
        scope: CoroutineScope
    ): Flow<Triple<Boolean, Boolean, Long>> {
        val userRef = FirebaseDatabase.getInstance().getReference("users/${uid}")
        val onlineFlow = MutableSharedFlow<Boolean>()
        val lastTimeFlow = MutableSharedFlow<Long>()
        val typingFlow = MutableSharedFlow<Boolean>()
        val onlineListener =
            userRef.child("online").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val online = snapshot.getValue(Int::class.java) ?: false
                    scope.launch(Dispatchers.IO) {
                        onlineFlow.emit(online == 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        val lastOnlineListener =
            userRef.child("lastOnline").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastOnline = snapshot.getValue(Long::class.java) ?: 0L
                    scope.launch(Dispatchers.IO) {
                        lastTimeFlow.emit(lastOnline)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        val typingRef =
            FirebaseDatabase.getInstance().reference.child("typing/$uid/$myUid")
        val typingListener =
            typingRef.child("started").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val typing = snapshot.getValue(Int::class.java) ?: 0
                    Log.i("test","snapshot ${snapshot.getValue(Int::class.java)}")
                    scope.launch(Dispatchers.IO) {
                        typingFlow.emit(typing == 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        scope.launch(Dispatchers.IO) {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                userRef.removeEventListener(onlineListener)
                userRef.removeEventListener(lastOnlineListener)
                userRef.removeEventListener(typingListener)
            }
        }
        return typingFlow.combine(onlineFlow) { typing, online -> Pair(typing, online) }
            .combine(lastTimeFlow) { p, time -> Triple(p.first, p.second, time) }
    }

    override suspend fun findByPhone(phone: String): List<User> = suspendCoroutine {
        Log.d(TAG, "findUsers")
        val users = ArrayList<User>()
        val respRef = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("phone")
            .equalTo(phone)
        respRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                    for (user in snapshot.children) users += userFromRef(user)
                respRef.removeEventListener(this)
                it.resume(users)
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.w(TAG, "Firebase request cancelled: $p0")
                it.resume(listOf())
            }
        })
    }

    override suspend fun receiveUsersByPhones(phones: List<String>): List<User> = suspendCoroutine {
        val phonesMap = HashMap<String, String>()
        for (phone in phones) phonesMap[phone] = ""
        val ref = FirebaseDatabase.getInstance().reference.child("requests").push()
        ref.setValue(phonesMap)
        val requestKey = ref.key
        val respRef = FirebaseDatabase.getInstance().getReference("responses/$requestKey")
        respRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != null) {
                    val users = ArrayList<User>()
                    for (child in snapshot.children) {
                        val user = userFromRef(child)
                        users.add(user)
                    }
                    respRef.removeValue()
                    respRef.removeEventListener(this)
                    it.resume(users)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.w(TAG, "Firebase request cancelled: $p0")
                it.resume(listOf())
            }
        })
    }

    override fun sendReport(messageId: String, received: Int, viewed: Int) {
        Log.i(TAG, "sendReport messageId $messageId")
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").child(messageId).updateChildren(
            mapOf(
                "received" to received.toString(),
                "viewed" to viewed.toString()
            )
        )
    }

    override fun sendTyping(myUid: String, uid: String, started: Int) {
        Log.i(TAG, "sendTyping started $started")
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("typing").child("${myUid}/$uid").setValue(mapOf("started" to started))
    }

    override fun sendMessage(
        message: Message?,
        uid: String,
        publicKey: String?,
        initiator: Boolean,
        onSuccess: () -> Unit
    ): String {
        Log.i(TAG, "firebase sendMessage")
        val ref = FirebaseDatabase.getInstance().reference.child("messages").push()
        ref.setValue(
            mapOf(
                "message" to message?.text + message?.time.toString(), //text end - temporary storage for timestamp
                "reply" to message?.replyMessageId,
                "image" to message?.image,
                "audio" to message?.audio,
                "dest" to uid,
                "source" to token,
                "key" to publicKey,
                "initiator" to if (initiator) 1 else 0
            )
        ).addOnFailureListener {
            Log.w(TAG, "sendMessage failure", it)
        }.addOnSuccessListener {
            GlobalScope.launch(Dispatchers.IO) { onSuccess() }
        }
        return ref.key!!
    }

    override fun renameUser(uid: String, name: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("name" to name))
    }

    override fun updateStatus(uid: String, status: String?) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("status" to status))
    }

    override fun updatePhoto(uid: String, photo: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("photo" to photo))
    }

    override suspend fun uploadFile(
        fileName: String,
        uid: String,
        byteArray: ByteArray,
        isPrivate: Boolean
    ): Boolean =
        suspendCoroutine { continuation ->
            val path = if (isPrivate) "/files/" else "/user/$uid/"
            val fileRef = FirebaseStorage.getInstance().getReference(path).child(fileName)
            Log.d(TAG, "start uploading $fileName to ${fileRef.path}")

            fileRef.putBytes(byteArray).addOnSuccessListener {
                Log.d(TAG, "uploaded $fileName")
                continuation.resume(true)
            }.addOnFailureListener {
                Log.w(TAG, "NOT uploaded $fileName $it")
                it.printStackTrace()
                continuation.resume(false)
            }
        }

    override suspend fun downloadFile(
        fileName: String,
        uid: String,
        isPrivate: Boolean
    ): ByteArray? =
        suspendCoroutine { continuation ->
            val path = if (isPrivate) "/files/" else "/user/$uid/"
            val fileRef = FirebaseStorage.getInstance()
                .getReference(path).child(fileName)
            Log.d(TAG, "start downloading: $fileName ref $fileRef")

            fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
                Log.d(TAG, "downloaded $fileName")
                if (isPrivate) fileRef.delete()
                continuation.resume(it)
            }.addOnFailureListener {
                Log.w(TAG, "NOT downloaded $fileName $it")
                continuation.resume(null)
            }
        }


    override fun deleteRemoteMessage(messageId: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").child(messageId).removeValue()
    }

    override suspend fun getUser(uid: String): User? = suspendCoroutine {
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

    override fun setOnline() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/${uid}")
            userRef.child("online").setValue(1)
            userRef.child("online").onDisconnect().setValue(0)
            userRef.child("lastOnline").onDisconnect().setValue(ServerValue.TIMESTAMP)
            database.getReference("typing/$uid").onDisconnect().removeValue()
        }
    }

    override fun setOffline() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/${uid}")
            userRef.child("online").setValue(0)
            userRef.child("lastOnline").setValue(System.currentTimeMillis())
            database.getReference("typing/$uid").removeValue()
        }
    }

    override suspend fun getToken(): String? = suspendCoroutine {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getToken() failed", task.exception)
                    it.resume(null)
                } else {
                    val token = task.result!!.token
                    Log.d(TAG, "token $token")
                    it.resume(token)
                }
            }
    }

    private fun userFromRef(snapshot: DataSnapshot): User {
        val uid = snapshot.key!!
        val name = snapshot.child("name").value?.toString() ?: ""
        val phone = snapshot.child("phone").value.toString()
        val status = snapshot.child("status").value?.toString()
        val photo = snapshot.child("photo").value?.toString()
        return User(uid = uid, phone = phone, name = name, status = status, photo = photo)
    }
}

