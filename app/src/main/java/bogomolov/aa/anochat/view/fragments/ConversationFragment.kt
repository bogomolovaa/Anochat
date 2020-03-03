package bogomolov.aa.anochat.view.fragments


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationBinding
import bogomolov.aa.anochat.view.MessagesPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationViewModel
import com.google.android.material.card.MaterialCardView
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class ConversationFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ConversationViewModel by activityViewModels { viewModelFactory }


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
        binding.viewModel = viewModel
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        viewModel.conversationLiveData.observe(viewLifecycleOwner){
            (activity as AppCompatActivity).supportActionBar!!.title = it.user.name
        }

        val conversationId = arguments?.get("id") as Long
        val adapter = MessagesPagedAdapter()
        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.loadMessages(conversationId).observe(viewLifecycleOwner) {
            Log.i("test", "pagedListLiveData observed from conversationId ${conversationId}")
            adapter.submitList(it)
            binding.recyclerView.scrollToPosition(it.size - 1);
        }
        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom && adapter.itemCount > 0) {
                binding.recyclerView.postDelayed({
                    binding.recyclerView.smoothScrollToPosition(adapter.itemCount - 1);
                }, 100)
            }
        }

        binding.fab.setOnClickListener { v ->
            val text = binding.messageInputText.text
            if (!text.isNullOrEmpty()) {
                Log.i("test", "message text: $text")
                viewModel.sendMessage(text.toString())
                binding.messageInputText.setText("")
            }
        }

        return view
    }

}
