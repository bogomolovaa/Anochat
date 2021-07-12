package bogomolov.aa.anochat.features.conversations.search

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentMessageSearchBinding
import bogomolov.aa.anochat.features.conversations.list.ConversationsPagedAdapter
import bogomolov.aa.anochat.features.shared.bindingDelegate
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessageSearchFragment : Fragment(R.layout.fragment_message_search), UpdatableView<MessageSearchUiState> {
    private val viewModel: MessageSearchViewModel by viewModels()
    private lateinit var navController: NavController
    private val adapter = ConversationsPagedAdapter(showFullMessage = true)
    private val binding by bindingDelegate(FragmentMessageSearchBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        setHasOptionsMenu(true)
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun updateView(newState: MessageSearchUiState, currentState: MessageSearchUiState) {
        if (newState.pagingData != currentState.pagingData)
            adapter.submitData(lifecycle, newState.pagingData!!)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.users_menu, menu)
        val context = requireContext()
        val searchString = arguments?.getString("search")!!

        val searchView = SearchView(context)
        searchView.setOnSubmitListener { query -> viewModel.messageSearch(query) }
        searchView.setTextColor(R.color.title_color)
        searchView.setQuery(searchString, true)
        //val closeButton = searchView.findViewById(R.id.search_close_btn) as ImageView
        //closeButton.setOnClickListener { navController.navigateUp() }

        val menuItem = menu.findItem(R.id.action_search)
        menuItem.expandActionView()
        menuItem.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
        }
        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                navController.navigateUp()
                return true
            }
        })
        menuItem.expandActionView()
        searchView.setQuery(searchString, true)
    }
}

fun SearchView.setTextColor(colorId: Int) {
    val searchAutoComplete =
        findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
    searchAutoComplete.setHintTextColor(ContextCompat.getColor(context, colorId))
    searchAutoComplete.setTextColor(ContextCompat.getColor(context, colorId))
}

fun SearchView.setOnSubmitListener(onSubmit: (String) -> Unit) {
    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?) =
            if (query != null && query.length >= 3) {
                onSubmit(query)
                true
            } else false

        override fun onQueryTextChange(newText: String?) = false
    })
}