package bogomolov.aa.anochat.features.conversations.search

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentMessageSearchBinding
import bogomolov.aa.anochat.features.conversations.list.ConversationsPagedAdapter
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class MessageSearchFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: MessageSearchViewModel by activityViewModels { viewModelFactory }
    private lateinit var navController: NavController
    private val adapter = ConversationsPagedAdapter(showFullMessage = true)
    private var searchLiveData: LiveData<PagedList<Conversation>>? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentMessageSearchBinding>(
            inflater,
            R.layout.fragment_message_search,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        setHasOptionsMenu(true)

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            navController.navigateUp()
        }

        return binding.root
    }

    private fun search(searchString: String) {
        if (searchLiveData != null) searchLiveData!!.removeObservers(viewLifecycleOwner)
        searchLiveData = viewModel.search(searchString)
        searchLiveData!!.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.users_menu, menu)
        val searchView = SearchView(requireContext())
        val menuItem = menu.findItem(R.id.action_search)
        menuItem.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.length >= 3) {
                    search(query)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && newText.length >= 3) {
                    search(newText)
                    return true
                }
                return false
            }
        })
        val closeButton = searchView.findViewById(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            navController.navigateUp()
        }
        val searchString = arguments?.getString("search")!!
        menuItem.expandActionView()
        searchView.setQuery(searchString, true)
        searchView.clearFocus()


        menuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem) = true

            override fun onMenuItemActionCollapse(item:MenuItem):Boolean {
                navController.navigateUp()
                return true
            }
        })

    }

}
