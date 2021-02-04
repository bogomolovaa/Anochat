package bogomolov.aa.anochat.features.contacts.list


import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentUsersBinding
import bogomolov.aa.anochat.view.adapters.AdapterHelper
import bogomolov.aa.anochat.view.adapters.UsersAdapter
import bogomolov.aa.anochat.view.adapters.UsersSearchAdapter
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


class UsersFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: UsersViewModel by activityViewModels { viewModelFactory }
    lateinit var recyclerView: RecyclerView
    private var adapter: UsersAdapter? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentUsersBinding>(
            inflater,
            R.layout.fragment_users,
            container,
            false
        )
        binding.lifecycleOwner = this
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        val onClick = { user: User ->
            viewModel.createConversation(user) { conversationId ->
                navController.navigate(
                    R.id.conversationFragment,
                    Bundle().apply { putLong("id", conversationId) })
            }
        }
        adapter = UsersAdapter(AdapterHelper(onClick = onClick))
        val searchAdapter = UsersSearchAdapter(AdapterHelper(onClick = onClick))
        viewModel.searchLiveData.observe(viewLifecycleOwner) {
            recyclerView.adapter = searchAdapter
            searchAdapter.submitList(it)
        }
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val contacts = viewModel.getContacts()
            withContext(Dispatchers.Main) {
                viewModel.loadContactUsers(contacts).observe(viewLifecycleOwner) {
                    recyclerView.adapter = adapter
                    adapter!!.submitList(it)
                    binding.progressBar.visibility = View.INVISIBLE
                }
            }
        }

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.users_menu, menu)
        val searchView = SearchView(requireContext())
        menu.findItem(R.id.action_search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    viewModel.search(query)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        val closeButton = searchView.findViewById(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            recyclerView.adapter = adapter
        }

    }
}
