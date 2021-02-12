package bogomolov.aa.anochat.features.conversations.list


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
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
import bogomolov.aa.anochat.databinding.FragmentConversationsListBinding
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import bogomolov.aa.anochat.view.adapters.AdapterHelper
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
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_conversations_list,
            container,
            false
        )
        binding.lifecycleOwner = this
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        setHasOptionsMenu(true)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        binding.fab.setOnClickListener {
            requestContactsPermission()
        }

        val focus = requireActivity().currentFocus
        if (focus != null) {
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(focus.windowToken, 0)
        }

        setupRecyclerView()

        return binding.root
    }

    override fun updateView(newState: ConversationsUiState, currentState: ConversationsUiState) {
        if (newState.pagedListLiveData != currentState.pagedListLiveData) setPagedList(newState.pagedListLiveData!!)
    }

    private fun setPagedList(pagedListLiveData: LiveData<PagedList<Conversation>>) {
        pagedListLiveData.observe(viewLifecycleOwner) {
            val adapter = binding.recyclerView.adapter as ConversationsPagedAdapter
            adapter.submitList(it)
        }
    }

    private fun setupRecyclerView() {
        val actionsMap = HashMap<Int, (Set<Long>, Set<Conversation>) -> Unit>()
        actionsMap[R.id.delete_conversations_action] =
            { ids, items -> viewModel.addAction(DeleteConversationsAction(ids)) }
        val adapter =
            ConversationsPagedAdapter(helper =
            AdapterHelper(
                R.menu.conversations_menu,
                actionsMap,
                binding.toolbar
            ) {
                navController.navigate(
                    R.id.dialog_graph,
                    Bundle().apply { putLong("id", it.id) })
            })
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.main_menu, menu)

        val searchView = SearchView(requireContext())
        val searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_button) as ImageView
        //searchIcon.backgroundTintList =
        //    ContextCompat.getColorStateList(requireContext(), R.color.title_color)
        searchIcon.setImageDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.search_icon)
        )


        val searchAutoComplete =
            searchView.findViewById(androidx.appcompat.R.id.search_src_text) as SearchView.SearchAutoComplete
        searchAutoComplete.setHintTextColor(
            ContextCompat.getColor(requireContext(), R.color.title_color)
        )
        searchAutoComplete.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.title_color)
        )


        menu.findItem(R.id.search_messages_action).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.length >= 3) {
                    val bundle = Bundle().apply { putString("search", query) }
                    navController.navigate(R.id.messageSearchFragment, bundle)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (item.itemId == R.id.menu_sign_out) {
            viewModel.addAction(SignOutAction())
            navController.navigate(R.id.signInFragment)
            return true
        }
        return (NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CONTACTS_PERMISSIONS_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    navController.navigate(R.id.usersFragment)
                } else {
                    Log.i("test", "contacts perm not granted")
                }
            }
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

