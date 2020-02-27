package bogomolov.aa.anochat.repository

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import bogomolov.aa.anochat.core.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


interface IFirebaseRepository {
    fun findUsers(startWith: String): List<User>
    fun sendMessage(message: String, user: User)
    fun updateUser(user: User)
    suspend fun signUp(name: String, email: String, password: String): Boolean
    suspend fun signIn(email: String, password: String): Boolean
    fun signOut()
    suspend fun isSignedIn(): Boolean
}

class FirebaseRepository @Inject constructor(val context: Context) : IFirebaseRepository {
    private lateinit var token: String
    //  "dtJant07QZk:APA91bGiAIP5GiGb_LH4R13Jmz1F8njD5QXNcsr886I39btTCsgEjHYz1nP2ets45wWCCxLoGwfh8zOdlncS-HBKxahD0g-JEdfaQEvgY7b_siANa24HA5DMn9VRVD7XXAN_nL6tZqar";
    //TODO: turn on caching

    init {
        signOut()
        GlobalScope.launch {
            token = getToken()
        }
    }

    override fun findUsers(startWith: String): List<User> {
        val users = ArrayList<User>()
        val query = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("name")
            .startAt(startWith)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("test", "snapshot $snapshot")
                if (snapshot.exists()) {
                    for (user in snapshot.children) {
                        val uid = user.key!!
                        val name = user.child("name").value.toString()
                        val changed = user.child("changed").value.toString().toLong()
                        users += User(uid = uid, name = name, changed = changed)
                        Log.i("test", "uid $uid name $name changed ${Date(changed)}")
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.i("test", "DatabaseError $p0")
            }
        })
        return users
    }

    override fun sendMessage(message: String, user: User) {
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").push()
            .setValue(mapOf("message" to message, "dest" to user.uid, "source" to token))
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
                Log.i("test","FirebaseAuth.getInstance().currentUser null")
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val email = sharedPreferences.getString("email", "")!!
                val password = sharedPreferences.getString("password", "")!!
                if (email != "" && password != "")
                    return userSignIn(email, password) != null
            }
            Log.i("test","FirebaseAuth.getInstance().currentUser NOT null")
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