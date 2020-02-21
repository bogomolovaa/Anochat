package bogomolov.aa.anochat.view.fragments


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
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
        val binding = DataBindingUtil.inflate<>(inflater,R.layout.fragment_conversations_list,container,false)
        val view = binding.root

        return view
    }

}
