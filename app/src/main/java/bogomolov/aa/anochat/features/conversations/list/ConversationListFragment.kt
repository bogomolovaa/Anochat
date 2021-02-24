package bogomolov.aa.anochat.features.conversations.list

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationsListBinding
import bogomolov.aa.anochat.domain.entity.Conversation
import bogomolov.aa.anochat.features.shared.ActionModeData
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class ConversationListFragment : Fragment(), UpdatableView<ConversationsUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ConversationListViewModel by viewModels { viewModelFactory }
    private lateinit var navController: NavController
    private lateinit var binding: FragmentConversationsListBinding

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.addAction(InitConversationsAction())
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConversationsListBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        setHasOptionsMenu(true)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        binding.fab.setOnClickListener { requestContactsPermission() }
        hideKeyBoard()
        setupRecyclerView()

        return binding.root
    }

    override fun updateView(newState: ConversationsUiState, currentState: ConversationsUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData) setPagedList(newState.pagedListLiveData!!)
    }

    private fun setPagedList(pagedListLiveData: LiveData<PagedList<Conversation>>) {
        pagedListLiveData.observe(viewLifecycleOwner) {
            (binding.recyclerView.adapter as ConversationsPagedAdapter).submitList(it)
        }
    }

    private fun setupRecyclerView() {
        val data = ActionModeData<Conversation>(R.menu.conversations_menu, binding.toolbar)
        data.actionsMap[R.id.delete_conversations_action] =
            { ids, _ -> viewModel.addAction(DeleteConversationsAction(ids)) }
        val adapter = ConversationsPagedAdapter(
            actionModeData = data,
            onClickListener = { conversation, _ ->
                val bundle = Bundle().apply { putLong("id", conversation.id) }
                navController.navigate(R.id.dialog_graph, bundle)
            }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.main_menu, menu)
        val context = requireContext()

        val searchView = SearchView(context)
        searchView.setOnSubmitListener { query->
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

@BindingAdapter(value = ["android:textStyle"])
fun setTypeface(v: TextView, style: Int) {
    v.setTypeface(null, style);
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