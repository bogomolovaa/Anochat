package bogomolov.aa.anochat.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.core.Message
import bogomolov.aa.anochat.core.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


interface IFirebaseRepository {
    suspend fun findUsers(startWith: String): List<User>
    fun updateUser(user: User)
    suspend fun signUp(name: String, email: String, password: String): Boolean
    suspend fun signIn(email: String, password: String): Boolean
    fun signOut()
    suspend fun isSignedIn(): Boolean
    suspend fun sendMessage(message: Message, uid: String)
    suspend fun uploadFile(fileName: String): Boolean
    suspend fun downloadFile(fileName: String):Boolean
}

class FirebaseRepository @Inject constructor(val context: Context) : IFirebaseRepository {
    private lateinit var token: String
    //TODO: turn on caching

    init {
        //signOut()
        GlobalScope.launch {
            token = getToken()
        }
    }

    override suspend fun downloadFile(fileName: String):Boolean = suspendCoroutine { continuation ->
        Log.i("test", "start downloading: $fileName")
        val fileRef = FirebaseStorage.getInstance().reference.child(fileName)
        val localFile = File(getFilesDir(context), fileName)
        fileRef.getFile(localFile).addOnSuccessListener {
            Log.i("test", "downloaded $fileName")
            continuation.resume(true)
        }.addOnFailureListener {
            Log.i("test", "NOT downloaded $fileName")
            continuation.resume(false)
        }
    }

    override suspend fun uploadFile(fileName: String): Boolean = suspendCoroutine { continuation ->
        Log.i("test", "start uploading $fileName")
        val fileRef = FirebaseStorage.getInstance().reference.child(fileName)
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

    suspend fun getUser(uid: String): User = suspendCoroutine {
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                it.resume(userFromRef(snapshot))
            }

            override fun onCancelled(p0: DatabaseError) {
                it.resumeWithException(Exception("DatabaseError $p0"))
            }
        })
    }

    private fun userFromRef(snapshot: DataSnapshot): User {
        val uid = snapshot.key!!
        val name = snapshot.child("name").value.toString()
        val changed = snapshot.child("changed").value.toString().toLong()
        return User(uid = uid, name = name, changed = changed)
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

    override suspend fun sendMessage(message: Message, uid: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").push()
            .setValue(
                mapOf(
                    "message" to message.text,
                    "image" to message.image,
                    "dest" to uid,
                    "source" to token
                )
            )
    }

    override fun updateUser(user: User) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("users").child(user.uid).setValue(mapOf("changed" to user.changed))
        myRef.child("users").child(user.uid).setValue(mapOf("name" to user.name))
    }

    override suspend fun signUp(name: String, email: String, password: String): Boolean {
        val uid = userSignUp(email, password)
        if (uid == null) return false
        saveEmailAndPassword(email, password)
        val user = User(name = name, uid = uid, changed = System.currentTimeMillis())
        updateUser(user)
        saveUidAndToken(uid, token)
        return true
    }

    override suspend fun signIn(email: String, password: String): Boolean {
        val uid = userSignIn(email, password)
        if (uid == null) return false
        saveUidAndToken(uid, token)
        return true
    }

    override fun signOut() {
        FirebaseAuth.getInstance().signOut()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit { putString("uid", "") }
    }

    override suspend fun isSignedIn(): Boolean {
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("uid", "") == "") {
            return false
        } else {
            if (FirebaseAuth.getInstance().currentUser == null) {
                Log.i("test", "FirebaseAuth.getInstance().currentUser null")
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val email = sharedPreferences.getString("email", "")!!
                val password = sharedPreferences.getString("password", "")!!
                if (email != "" && password != "")
                    return userSignIn(email, password) != null
            }
            Log.i("test", "FirebaseAuth.getInstance().currentUser NOT null")
            return true
        }
    }

    private fun saveEmailAndPassword(email: String, password: String) {
        //TODO: encrypt password
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit { putString("email", email) }
        sharedPreferences.edit { putString("password", password) }
    }

    private fun saveUidAndToken(uid: String, token: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        //user changed
        if (uid != sharedPreferences.getString("uid", "")) {
            sharedPreferences.edit { putString("uid", uid) }
            sharedPreferences.edit { putString("token", "") }
        }
        //token changed
        if (token != sharedPreferences.getString("token", "")) {
            sharedPreferences.edit { putString("token", token) }
            val myRef = FirebaseDatabase.getInstance().reference
            myRef.child("user_tokens").child(uid).setValue(mapOf("token" to token))
        }
    }

    private suspend fun userSignUp(email: String, password: String): String? = suspendCoroutine {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.i(
                        "test",
                        "User created: ${user!!.displayName} email: ${user.email} uid: ${user.uid}"
                    )
                    it.resume(user.uid)
                } else {
                    Log.i("test", "User NOT created")
                    it.resume(null)
                }
            }
    }

    private suspend fun userSignIn(email: String, password: String): String? = suspendCoroutine {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.i(
                        "test",
                        "Authentication succeeded name: ${user!!.displayName} email: ${user.email} uid: ${user.uid}"
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