package bogomolov.aa.anochat.view.fragments


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.core.Conversation
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentConversationsListBinding
import bogomolov.aa.anochat.view.adapters.AdapterHelper
import bogomolov.aa.anochat.view.adapters.ConversationsPagedAdapter
import bogomolov.aa.anochat.viewmodel.ConversationListViewModel
import com.vanniktech.emoji.EmojiTextView
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class ConversationsListFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ConversationListViewModel by activityViewModels { viewModelFactory }
    private lateinit var navController: NavController

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

        Log.i("test", "ConversationsListFragment onCreateView")

        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        val actionsMap = HashMap<Int, (Set<Long>, Set<Conversation>) -> Unit>()
        actionsMap[R.id.delete_conversations_action] =
            { ids, items -> viewModel.deleteConversations(ids) }
        val adapter =
            ConversationsPagedAdapter(helper =
            AdapterHelper(
                R.menu.conversations_menu,
                actionsMap,
                binding.toolbar
            ) {
                Log.i("test", "navigate to R.id.conversationFragment")
                navController.navigate(
                    R.id.conversationFragment,
                    Bundle().apply { putLong("id", it.id) })
            })
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        viewModel.loadConversations().observe(viewLifecycleOwner) {
            Log.i("Test", "pagedListLiveData loaded")
            adapter.submitList(it)
        }


        NavigationUI.setupWithNavController(binding.toolbar, navController)
        binding.fab.setOnClickListener {
            requestContactsPermission()
        }

        val view = requireActivity().currentFocus
        if (view != null) {
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.main_menu, menu)

        val searchView = SearchView(requireContext())
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
            viewModel.signOut()
            navController.navigate(R.id.conversationsListFragment)
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
                    Log.i("test", "PERMISSION_GRANTED")
                    navController.navigate(R.id.usersFragment)
                } else {
                    Log.i("test", "contacts perm not granted")
                }
            }
        }
    }

    private fun requestContactsPermission() {
        Log.i("test", "requestContactsPermission")
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

