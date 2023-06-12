package bogomolov.aa.anochat.repository

import android.util.Log
import bogomolov.aa.anochat.domain.MessagesListener
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FirebaseRepository"

private enum class MessageType {
    MESSAGE,
    REPORT,
    KEY,
}


@Singleton
class FirebaseImpl @Inject constructor() : Firebase {
    private lateinit var token: String
    private val firebaseScope = CoroutineScope(Dispatchers.IO)
    private var removeListener: (() -> Unit)? = null
    private val DOWNLOAD_TIMEOUTS = listOf(1, 3, 10, 30, 60, 300)

    init {
        firebaseScope.launch {
            //FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG)
            //FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            updateToken()
        }
    }

    override fun removeMessagesListener() {
        removeListener?.invoke()
        removeListener = null
    }

    override fun setMessagesListener(listener: MessagesListener) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(TAG, "MessagesListener started myUid $myUid")
        val ref = FirebaseDatabase.getInstance().getReference("notifications/${myUid}")
        val childEventListener = ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "MessagesListener notifications/${myUid}: $snapshot")
                val notification = snapshot.value as Map<String, String>
                val messageId = notification["message"]
                val uid = notification["source"]
                if (messageId != null && uid != null) {
                    val messageRef =
                        FirebaseDatabase.getInstance().reference.child("messages/$uid/$myUid/$messageId")
                    messageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            Log.d(
                                TAG,
                                "MessagesListener messages/$uid/$myUid/$messageId: $snapshot"
                            )
                            val data = snapshot.value as Map<String, Any>
                            val type = MessageType.valueOf(data["type"] as String)
                            when (type) {
                                MessageType.MESSAGE ->
                                    receiveMessage(listener, uid, messageId, data)
                                MessageType.REPORT -> receiveReport(listener, data)
                                MessageType.KEY -> receiveKey(listener, uid, data)
                            }
                            messageRef.removeValue()
                        }

                        override fun onCancelled(p0: DatabaseError) {
                        }
                    })
                }
                snapshot.key?.let { ref.child(it).removeValue() }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        removeListener = {
            ref.removeEventListener(childEventListener)
        }
    }

    private fun receiveMessage(
        messagesListener: MessagesListener,
        uid: String,
        messageId: String,
        data: Map<String, Any>
    ) {
        firebaseScope.launch {
            val message = Message(
                text = data["message"] as String,
                time = data["time"] as Long,
                messageId = messageId,
                replyMessageId = data["reply"] as String?,
                image = data["image"] as String?,
                audio = data["audio"] as String?,
                video = data["video"] as String?
            )
            messagesListener.onMessageReceived(message, uid)
        }
    }

    private fun receiveReport(messagesListener: MessagesListener, data: Map<String, Any>) {
        firebaseScope.launch {
            //if (viewed == 1 || received == -1) deleteRemoteMessage(messageId)
            val messageId = data["message"] as String
            val viewed = data["viewed"] as Long
            val received = data["received"] as Long
            Log.d(TAG, "receivedReport received $received viewed $viewed")
            messagesListener.onReportReceived(messageId, received.toInt(), viewed.toInt())
        }
    }

    private fun receiveKey(
        messagesListener: MessagesListener,
        uid: String,
        data: Map<String, Any>
    ) {
        firebaseScope.launch {
            val publicKey = data["key"] as String
            val initiator = data["initiator"] as Boolean
            //deleteRemoteMessage(messageId)
            messagesListener.onPublicKeyReceived(uid, publicKey, initiator)
        }
    }

    override suspend fun addUserStatusListener(
        myUid: String,
        uid: String
    ): Flow<Triple<Boolean, Boolean, Long>> {
        val userRef = FirebaseDatabase.getInstance().getReference("users/${uid}")
        val onlineFlow = MutableSharedFlow<Boolean>(replay = 1)
        val lastTimeFlow =
            MutableSharedFlow<Long>(replay = 1)
        val typingFlow = MutableSharedFlow<Boolean>(replay = 1)
        firebaseScope.launch {
            val onlineListener =
                userRef.child("online").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val online = snapshot.getValue(Int::class.java) ?: false
                        onlineFlow.tryEmit(online == 1)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            val lastOnlineListener =
                userRef.child("lastOnline").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val lastOnline = snapshot.getValue(Long::class.java) ?: 0L
                        lastTimeFlow.tryEmit(lastOnline)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            val typingRef =
                FirebaseDatabase.getInstance().reference.child("typing/$uid/$myUid")
            val typingListener =
                typingRef.child("started").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val typing = snapshot.getValue(Int::class.java) ?: 0
                        Log.i("test", "snapshot ${snapshot.getValue(Int::class.java)}")
                        typingFlow.tryEmit(typing == 1)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            try {
                delay(Long.MAX_VALUE)
            } finally {
                userRef.removeEventListener(onlineListener)
                userRef.removeEventListener(lastOnlineListener)
                typingRef.removeEventListener(typingListener)
            }
        }
        return combine(typingFlow, onlineFlow, lastTimeFlow) { typing, online, lastTime ->
            Triple(typing, online, lastTime)
        }
    }

    override suspend fun findByPhone(phone: String): List<User> = suspendCancellableCoroutine {
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

    override suspend fun receiveUsersByPhones(phones: List<String>): List<User> =
        suspendCancellableCoroutine {
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

    override fun sendMessage(
        message: Message?,
        uid: String,
        onSuccess: () -> Unit
    ) = send(type = MessageType.MESSAGE, message = message, uid = uid, onSuccess = onSuccess)

    override fun sendReport(
        messageId: String,
        uid: String,
        received: Int,
        viewed: Int
    ) {
        send(
            type = MessageType.REPORT,
            messageId = messageId,
            uid = uid,
            received = received,
            viewed = viewed
        )
    }

    override fun sendKey(
        uid: String,
        publicKey: String?,
        initiator: Boolean
    ) {
        send(
            type = MessageType.KEY,
            uid = uid,
            publicKey = publicKey,
            initiator = initiator
        )
    }

    private fun send(
        type: MessageType,
        message: Message? = null,
        messageId: String? = null,
        uid: String = "",
        received: Int = 0,
        viewed: Int = 0,
        publicKey: String? = null,
        initiator: Boolean = false,
        onSuccess: () -> Unit = {}
    ): String {
        Log.i(TAG, "firebase send ${type.name}")
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        val ref =
            FirebaseDatabase.getInstance().reference.child("messages").child("${myUid}/$uid").push()
        ref.setValue(
            when (type) {
                MessageType.MESSAGE -> mapOf(
                    "type" to type.name,
                    "message" to message?.text,
                    "time" to message?.time,
                    "reply" to message?.replyMessageId,
                    "image" to message?.image,
                    "video" to message?.video,
                    "audio" to message?.audio
                )
                MessageType.REPORT -> mapOf(
                    "type" to type.name,
                    "message" to messageId,
                    "received" to received,
                    "viewed" to viewed
                )
                MessageType.KEY -> mapOf(
                    "type" to type.name,
                    "key" to publicKey,
                    "initiator" to initiator,
                )
            }
        ).addOnFailureListener {
            Log.w(TAG, "send ${type.name} failure", it)
        }.addOnSuccessListener {
            val notifyRef =
                FirebaseDatabase.getInstance().reference.child("notifications").child(uid).push()
            notifyRef.setValue(
                mapOf(
                    "message" to ref.key,
                    "source" to myUid,
                )
            ).addOnFailureListener {
                Log.w(TAG, "notify ${type.name} failure", it)
            }.addOnSuccessListener {
                firebaseScope.launch { onSuccess() }
            }
        }
        return ref.key!!
    }

    override fun deleteRemoteMessage(messageId: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").child(messageId).removeValue()
    }

    override fun sendTyping(myUid: String, uid: String, started: Int) {
        Log.i(TAG, "sendTyping started $started")
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("typing").child("${myUid}/$uid").setValue(mapOf("started" to started))
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
    ): ByteArray? {
        repeat(DOWNLOAD_TIMEOUTS.size) {
            val result = tryDownloadFile(fileName, uid, isPrivate)
            if (result != null) return result
            else delay(DOWNLOAD_TIMEOUTS[it] * 1000L)
        }
        return null
    }


    private suspend fun tryDownloadFile(
        fileName: String,
        uid: String,
        isPrivate: Boolean
    ): ByteArray? = suspendCancellableCoroutine { continuation ->
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

    override suspend fun getUser(uid: String): User? = suspendCancellableCoroutine {
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

    override suspend fun updateToken(): String? = suspendCoroutine {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "getToken() failed", task.exception)
                it.resume(null)
            } else {
                val newToken = task.result
                Log.d(TAG, "token $newToken")
                token = newToken
                it.resume(newToken)
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