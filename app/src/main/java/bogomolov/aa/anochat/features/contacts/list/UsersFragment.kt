package bogomolov.aa.anochat.features.contacts.list

import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
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
import bogomolov.aa.anochat.databinding.FragmentUsersBinding
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import bogomolov.aa.anochat.repository.isValidPhone
import bogomolov.aa.anochat.view.adapters.AdapterHelper
import bogomolov.aa.anochat.view.adapters.UsersAdapter
import bogomolov.aa.anochat.view.adapters.UsersSearchAdapter
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class UsersFragment : Fragment(), UpdatableView<ContactsUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: UsersViewModel by viewModels { viewModelFactory }

    private lateinit var navController: NavController
    private lateinit var binding: FragmentUsersBinding
    private val usersAdapter = UsersAdapter(AdapterHelper(onClick = getOnClick()))
    private val searchAdapter = UsersSearchAdapter(AdapterHelper(onClick = getOnClick()))

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

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
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_users,
            container,
            false
        )
        binding.lifecycleOwner = this
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        setHasOptionsMenu(true)
        showProgressBar()

        return binding.root
    }

    override fun updateView(newState: ContactsUiState, currentState: ContactsUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData) setPagedList(newState.pagedListLiveData!!)
        if (newState.searchedUsers != currentState.searchedUsers) setSearchedUsers(newState.searchedUsers!!)
        if (newState.conversationId != currentState.conversationId) navigateToConversation(newState.conversationId)
        if (newState.synchronizationFinished != currentState.synchronizationFinished){
            hideProgressBar()
            setPagedList(newState.pagedListLiveData!!)
        }
    }

    private fun navigateToConversation(conversationId: Long) {
        if (conversationId != 0L) {
            navController.navigate(
                R.id.dialog_graph,
                Bundle().apply { putLong("id", conversationId) })
            viewModel.setStateAsync { copy(conversationId = 0) }
        }
    }

    private fun setPagedList(pagedListLiveData: LiveData<PagedList<User>>) {
        binding.recyclerView.adapter = usersAdapter
        pagedListLiveData.observe(viewLifecycleOwner) {
            usersAdapter.submitList(it)
        }
    }

    private fun setSearchedUsers(users: List<User>) {
        binding.recyclerView.adapter = searchAdapter
        searchAdapter.submitList(users)
    }

    private fun getOnClick() = { user: User ->
        viewModel.addAction(CreateConversationAction(user))
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun getContactsPhones(): List<String> {
        val phones = HashSet<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val numberIndex =
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex)
                val clearNumber =
                    number.replace("[- ()]".toRegex(), "").replace("^8".toRegex(), "+7")
                if (isValidPhone(clearNumber)) phones += clearNumber
            }
        }
        return ArrayList(phones)
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
                    viewModel.addAction(SearchAction(query))
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
            binding.recyclerView.adapter = usersAdapter
        }

    }
}
