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
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationsListBinding
import bogomolov.aa.anochat.view.ConversationsPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationListViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class ConversationsListFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentConversationsListBinding>(inflater,R.layout.fragment_conversations_list,container,false)
        val view = binding.root
        val viewModel = ViewModelProvider(this,viewModelFactory).get(ConversationListViewModel::class.java)

        val adapter = ConversationsPagedAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.pagedListLiveData.observe(viewLifecycleOwner){
            adapter.submitList(it)
        }

        return view
    }

}
