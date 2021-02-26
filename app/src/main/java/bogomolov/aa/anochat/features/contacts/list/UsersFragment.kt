package bogomolov.aa.anochat.features.contacts.list

import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentUsersBinding
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.entity.isValidPhone
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsersFragment : Fragment(), UpdatableView<ContactsUiState> {
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var binding: FragmentUsersBinding
    private val usersAdapter = UsersAdapter { user, _ -> createConversation(user) }
    private val searchAdapter = UsersSearchAdapter { user, _ -> createConversation(user) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.addAction(LoadContactsAction(getContactsPhones()))
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUsersBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        setHasOptionsMenu(true)

        return binding.root
    }

    override fun updateView(newState: ContactsUiState, currentState: ContactsUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData)
            setContactsPagedList(newState.pagedListLiveData!!)
        if (newState.searchedUsers != currentState.searchedUsers) setSearchedUsers(newState.searchedUsers!!)
        if (newState.conversationId != currentState.conversationId) navigateToConversation(newState.conversationId)
        if (newState.usersUpdated) usersAdapter.notifyDataSetChanged()
        binding.progressBar.visibility = if (newState.loading) View.VISIBLE else View.INVISIBLE
    }


    private fun setContactsPagedList(pagedListLiveData: LiveData<PagedList<User>>? = null) {
        binding.recyclerView.adapter = usersAdapter
        pagedListLiveData?.observe(viewLifecycleOwner) {
            usersAdapter.submitList(it)
        }
    }

    private fun setSearchedUsers(users: List<User>) {
        binding.recyclerView.adapter = searchAdapter
        searchAdapter.submitList(users)
    }

    private fun navigateToConversation(conversationId: Long) {
        if (conversationId != 0L) {
            val bundle = Bundle().apply { putLong("id", conversationId) }
            viewModel.setStateAsync { copy(conversationId = 0) }
            navController.navigate(R.id.dialog_graph, bundle)
        }
    }

    private fun createConversation(user: User) {
        viewModel.addAction(CreateConversationAction(user))
    }

    private fun getContactsPhones(): List<String> {
        val phones = HashSet<String>()
        val cursor = queryContacts()
        if (cursor != null) {
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex)
                val clearNumber =
                    number.replace("[- ()]".toRegex(), "").replace("^8".toRegex(), "+7")
                if (isValidPhone(clearNumber)) phones += clearNumber
            }
        }
        return ArrayList(phones)
    }

    private fun queryContacts(): Cursor? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        return requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )
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
            override fun onQueryTextSubmit(query: String?) =
                if (query != null) {
                    viewModel.addAction(SearchAction(query))
                    true
                } else false

            override fun onQueryTextChange(newText: String?) = false
        })
        val closeButton = searchView.findViewById(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener { setContactsPagedList() }
    }
}