package bogomolov.aa.anochat.view.fragments


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.view.MessagesPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

import android.content.DialogInterface
import android.util.Log

import android.widget.Toast

import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


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
        }

        binding.messageInputLayout.setEndIconOnClickListener { v ->
            val text = binding.messageInputText.text
            if (text != null) {
                viewModel.onNewMessage(text.toString())
                binding.messageInputText.setText("")
            }
        }

        signIn(inflater)


        return view
    }

    fun testDb(){
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("message")

        myRef.setValue("Hello, World!")

        /*
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference cineIndustryRef = rootRef.child("cineIndustry").push();
            String key = cineIndustryRef.getKey();
            Map<String, Object> map = new HashMap<>();
            map.put(key, "Hollywood");
            //and os on
            cineIndustryRef.updateChildren(map);
         */
    }

    fun testUser(email: String, password: String) {
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
                    testDb();
                } else {
                    Toast.makeText(
                        context, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    fun signIn(inflater: LayoutInflater) {
        val alert: AlertDialog.Builder = AlertDialog.Builder(context!!)
        val view = inflater.inflate(R.layout.test_sign_in, null)
        alert.setView(view)
        alert.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, whichButton: Int) {
                val inputEmail = view.findViewById<EditText>(R.id.email_input)
                val inputPassword = view.findViewById<EditText>(R.id.password_input)
                testUser(inputEmail.text.toString(), inputPassword.text.toString())
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
