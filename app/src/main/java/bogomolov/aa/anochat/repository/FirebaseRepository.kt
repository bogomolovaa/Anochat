package bogomolov.aa.anochat.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import bogomolov.aa.anochat.android.*
import bogomolov.aa.anochat.core.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import javax.crypto.SecretKey
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


interface IFirebaseRepository {
    suspend fun receiveUsersByPhones(phones: List<String>): List<User>
    suspend fun findByPhone(phone: String): List<User>
    suspend fun signUp(name: String, email: String, password: String): Boolean
    suspend fun signIn(phoneNumber: String, credential: PhoneAuthCredential): Boolean
    fun signOut()
    fun isSignedIn(): Boolean
    suspend fun uploadFile(
        fileName: String,
        uid: String? = null,
        needEncrypt: Boolean = false
    ): Boolean

    suspend fun downloadFile(
        fileName: String,
        uid: String? = null,
        needDecrypt: Boolean = false
    ): Boolean

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

    override suspend fun downloadFile(
        fileName: String,
        uid: String?,
        needDecrypt: Boolean
    ): Boolean =
        suspendCoroutine { continuation ->
            Log.i("test", "start downloading: $fileName")
            val fileRef = FirebaseStorage.getInstance()
                .getReference(if (!needDecrypt) "/user/{$uid}/" else "/files/").child(fileName)
            val localFile = File(getFilesDir(context), fileName)
            fileRef.getFile(localFile).addOnSuccessListener {
                Log.i("test", "downloaded $fileName")
                if (needDecrypt) decryptFile(localFile, uid!!, context)
                fileRef.delete()
                continuation.resume(true)
            }.addOnFailureListener {
                Log.i("test", "NOT downloaded $fileName")
                continuation.resume(false)
            }
        }

    override suspend fun uploadFile(fileName: String, uid: String?, needEncrypt: Boolean): Boolean =
        suspendCoroutine { continuation ->
            val fileRef = FirebaseStorage.getInstance()
                .getReference(if (!needEncrypt) "/user/$uid/" else "/files/").child(fileName)
            Log.i("test", "start uploading $fileName to ${fileRef.path} myUid ${getMyUid(context)}")
            val localFile = File(getFilesDir(context), fileName)
            val byteArray =
                if (needEncrypt) encryptFile(localFile, uid!!, context) else localFile.readBytes()
            if (byteArray == null) continuation.resumeWithException(Exception("not uploaded: can't read file $fileName"))
            fileRef.putBytes(byteArray!!).addOnSuccessListener {
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

    override suspend fun findByPhone(phone: String): List<User> = suspendCoroutine {
        val users = ArrayList<User>()
        val query = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("phone")
            .equalTo(phone)
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

    override suspend fun receiveUsersByPhones(phones: List<String>): List<User> = suspendCoroutine {
        val phonesMap = HashMap<String, String>()
        for (phone in phones) phonesMap[phone] = ""
        val ref = FirebaseDatabase.getInstance().reference.child("requests").push()
        ref.setValue(phonesMap)
        val requestKey = ref.key
        Log.i("test", "respRef.addValueEventListener")
        val respRef = FirebaseDatabase.getInstance().getReference("responses/$requestKey")
        respRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("test", "onDataChange $snapshot")
                if (snapshot.value != null) {
                    val users = ArrayList<User>()
                    for (child in snapshot.children) {
                        val user = userFromRef(child)
                        users.add(user)
                        Log.i("test", "response $user")
                    }
                    respRef.removeEventListener(this)
                    respRef.removeValue()
                    //key = -M2ZVxOnfsdtB-Hx1Cn_, value = {IVmG8808LGdtVPaz95CLIYIpI2D3={phone=+79057148736, name=sasha, online=1, lastOnline=1584379350147, photo=ipDgwQNFOmVCJlIvkxuu.jpg}}
                    it.resume(users)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                it.resume(listOf())
                Log.i("test", "onCancelled $p0")
            }
        })
    }

    fun sendMessage(
        text: String?,
        replyId: String?,
        image: String?,
        audio: String?,
        uid: String,
        publicKey: String?,
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

    suspend fun updatePhoto(uid: String, photo: String) {
        Log.i("test", "updatePhoto $uid $photo")
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(uid).updateChildren(mapOf("photo" to photo))
        uploadFile(photo, uid)
        uploadFile(getMiniPhotoFileName(context, photo), uid)
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
        setSetting<String>(context, UID, null)
    }

    override fun isSignedIn() = getSetting<String>(context, UID) != null && getUid() != null


    private fun saveUidAndToken(uid: String) {
        Log.i("test", "saveUidAndToken $uid")
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