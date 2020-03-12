package bogomolov.aa.anochat.view.fragments


import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationsListBinding
import bogomolov.aa.anochat.view.AdapterHelper
import bogomolov.aa.anochat.view.ConversationsPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationListViewModel
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import bogomolov.aa.anochat.R

class ConversationsListFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ConversationListViewModel by activityViewModels { viewModelFactory }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentConversationsListBinding>(
            inflater,
            R.layout.fragment_conversations_list,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        setHasOptionsMenu(true)

        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        val adapter = ConversationsPagedAdapter(AdapterHelper {
            navController.navigate(
                R.id.conversationFragment,
                Bundle().apply { putLong("id", it.id) })
        })
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.pagedListLiveData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }


        NavigationUI.setupWithNavController(binding.toolbar, navController)
        binding.fab.setOnClickListener {
            navController.navigate(R.id.usersFragment)
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        return (NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item))
    }


}
