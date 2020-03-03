package bogomolov.aa.anochat.view.fragments


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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

    //https://stackoverflow.com/questions/30699302/android-design-support-library-expandable-floating-action-buttonfab-menu


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
        viewModel.conversationLiveData.observe(viewLifecycleOwner) {
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

        val sendMessageAction = {
            val text = binding.messageInputText.text
            if (!text.isNullOrEmpty()) {
                Log.i("test", "message text: $text")
                viewModel.sendMessage(text.toString())
                binding.messageInputText.setText("")
            }
        }
        var fabExpanded = false
        var textEntered = false
        binding.fab.setOnClickListener {
            if (textEntered) {
                sendMessageAction()
                textEntered = false
            } else {
                if (!fabExpanded) {
                    binding.fab1.visibility = View.VISIBLE
                    binding.fab1.animate()
                        .translationY(50f).setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(var1: Animator) {
                                fabExpanded = true
                                binding.fab.setImageResource(R.drawable.plus_icon)
                            }
                        }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()

                } else {
                    binding.fab1.animate()
                        .translationY(0f).setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(var1: Animator) {
                                binding.fab1.visibility = View.INVISIBLE
                                fabExpanded = false
                                binding.fab.setImageResource(R.drawable.send_arrow)
                            }
                        }).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
                }
            }
        }

        binding.fab1.setOnClickListener {
            //select media

        }

        return view
    }

}
