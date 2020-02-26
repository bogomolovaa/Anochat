package bogomolov.aa.anochat.view.fragments


import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.view.MessagesPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class ConversationFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: ConversationViewModel

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentConversationBinding>(
            inflater,
            R.layout.fragment_conversation,
            container,
            false
        )
        val view = binding.root
        viewModel = ViewModelProvider(this, viewModelFactory).get(ConversationViewModel::class.java)
        binding.viewModel = viewModel

        val conversationId = 0L;
        val adapter = MessagesPagedAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.loadMessages(conversationId)
        viewModel.pagedListLiveData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.recyclerView.scrollToPosition(it.size - 1);
        }
        binding.recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom && adapter.itemCount > 0) {
                binding.recyclerView.postDelayed({
                    binding.recyclerView.smoothScrollToPosition(adapter.itemCount - 1);
                }, 100)
            }
        }

        binding.messageInputLayout.setEndIconOnClickListener { v ->
            val text = binding.messageInputText.text
            if (text != null) {
                Log.i("test", "message text: $text")
                //viewModel.onNewMessage(text.toString())
                binding.messageInputText.setText("")
                testMessage(text.toString())
            }
        }

        signInDialog(inflater)


        return view
    }


    fun findUser(input: String) {
        Log.i("test", "findUser $input")
        val query = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("name")
            .startAt(input)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("test", "snapshot $snapshot")
                if (snapshot.exists()) {
                    for (user in snapshot.children) {
                        val uid = user.key
                        val name = user.child("name").value
                        Log.i("test", "uid $uid name $name")
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.i("test", "DatabaseError $p0")
            }
        })
    }

    fun testMessage(message: String) {
        val uid = "LX4U2yR5ZJUsN5hivvDvF9NUHXJ3"
        val token =
            "dtJant07QZk:APA91bGiAIP5GiGb_LH4R13Jmz1F8njD5QXNcsr886I39btTCsgEjHYz1nP2ets45wWCCxLoGwfh8zOdlncS-HBKxahD0g-JEdfaQEvgY7b_siANa24HA5DMn9VRVD7XXAN_nL6tZqar";
        val myRef = FirebaseDatabase.getInstance().reference
        myRef.child("messages").push()
            .setValue(mapOf("message" to message, "dest" to uid, "source" to token))
    }

    fun updateTokenAndUid(email: String, password: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val uid = userSignIn(email, password)
            PreferenceManager.getDefaultSharedPreferences(context).edit { putString("uid", uid) }
            val token = getToken()
            updateNameTokenAndUid(token, uid)
            findUser("user")
        }
    }

    companion object {
        fun updateNameTokenAndUid(token: String, uid: String) {
            val myRef = FirebaseDatabase.getInstance().reference
            myRef.child("users").child(uid).setValue(mapOf("name" to "user1"))
            myRef.child("user_tokens").child(uid).setValue(mapOf("token" to token))
        }

        fun updateToken(token: String, uid: String) {
            val myRef = FirebaseDatabase.getInstance().reference
            myRef.child("user_tokens").child(uid).setValue(mapOf("token" to token))
        }
    }

    private suspend fun getToken(): String = suspendCoroutine {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful)
                    it.resumeWithException(Exception("getInstanceId failed"))
                val token = task.result!!.token
                Log.d("test", "token $token")
                it.resume(token)
            })
    }

    private suspend fun userSignIn(email: String, password: String): String = suspendCoroutine {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity!!) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(context, "Authentication succeeded", Toast.LENGTH_SHORT).show()
                    Log.i(
                        "test",
                        "Authentication succeeded name: ${user!!.displayName} email: ${user.email} uid: ${user.uid}"
                    )
                    it.resume(user.uid)
                } else {
                    Toast.makeText(
                        context, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    fun signInDialog(inflater: LayoutInflater) {
        val alert: AlertDialog.Builder = AlertDialog.Builder(context!!)
        val view = inflater.inflate(R.layout.test_sign_in, null)
        alert.setView(view)
        alert.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, whichButton: Int) {
                val inputEmail = view.findViewById<EditText>(R.id.email_input)
                val inputPassword = view.findViewById<EditText>(R.id.password_input)
                updateTokenAndUid(inputEmail.text.toString(), inputPassword.text.toString())
            }
        })
        alert.setNegativeButton(
            "Cancel",
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, whichButton: Int) {
                    dialog.cancel()
                }
            })
        alert.show()
    }

}
