package bogomolov.aa.anochat.features.conversations.list

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentConversationsListBinding
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.ActionModeData
import bogomolov.aa.anochat.features.shared.bindingDelegate
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ConversationListFragment : Fragment(R.layout.fragment_conversations_list), UpdatableView<ConversationsUiState> {
    val viewModel: ConversationListViewModel by viewModels()
    private lateinit var navController: NavController
    private val binding by bindingDelegate(FragmentConversationsListBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = Fade().apply { duration = 375 }
        requireActivity().window.decorView.setBackgroundResource(R.color.conversation_background)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        setHasOptionsMenu(true)
        navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        binding.fab.setOnClickListener { requestContactsPermission() }
        //hideKeyBoard()
        setupRecyclerView()
    }

    override fun updateView(newState: ConversationsUiState, currentState: ConversationsUiState) {
        if (newState.pagingData != currentState.pagingData)
            (binding.recyclerView.adapter as ConversationsPagedAdapter)
                .submitData(lifecycle, newState.pagingData!!)
    }

    private fun setupRecyclerView() {
        val data = ActionModeData<Conversation>(R.menu.conversations_menu, binding.toolbar)
        data.actionsMap[R.id.delete_conversations_action] =
            { ids, _ -> viewModel.addAction(DeleteConversationsAction(ids)) }
        val adapter =
            ConversationsPagedAdapter(actionModeData = data) { conversation ->
                val bundle = Bundle().apply { putLong("id", conversation.id) }
                navController.navigate(R.id.dialog_graph, bundle)
            }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.conversation_divider)!!
        )
        binding.recyclerView.addItemDecoration(itemDecorator)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.main_menu, menu)
        val context = requireContext()

        val searchView = SearchView(context)
        searchView.setOnSubmitListener { query ->
            val bundle = Bundle().apply { putString("search", query) }
            navController.navigate(R.id.messageSearchFragment, bundle)
        }
        searchView.setTextColor(R.color.title_color)
        val searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_button) as ImageView
        searchIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.search_icon))

        menu.findItem(R.id.search_messages_action).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_sign_out) {
            viewModel.addAction(SignOutAction())
            navController.navigate(R.id.signInFragment)
            return true
        }
        return NavigationUI.onNavDestinationSelected(item, navController)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            if (requestCode == CONTACTS_PERMISSIONS_CODE) navController.navigate(R.id.usersFragment)
    }

    private fun hideKeyBoard() {
        val focus = requireActivity().currentFocus
        if (focus != null) {
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(focus.windowToken, 0)
        }
    }

    private fun requestContactsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(CONTACTS_PERMISSIONS), CONTACTS_PERMISSIONS_CODE)
    }

    companion object {
        private const val CONTACTS_PERMISSIONS = Manifest.permission.READ_CONTACTS
        private const val CONTACTS_PERMISSIONS_CODE = 1001
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