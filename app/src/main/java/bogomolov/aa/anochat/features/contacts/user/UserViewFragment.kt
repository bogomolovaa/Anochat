package bogomolov.aa.anochat.features.contacts.user

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.NavigationUI
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentUserViewBinding
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.shared.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.UpdatableView
import bogomolov.aa.anochat.repository.getFilePath
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class UserViewFragment : Fragment(), UpdatableView<UserUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: UserViewViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentUserViewBinding

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = arguments?.getLong("id")!!
        viewModel.addAction(LoadUserAction(userId))
        viewModel.addAction(LoadImagesAction(userId))
        lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_user_view,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        setMainPhotoListener(navController)
        postponeEnterTransition()

        return binding.root
    }

    override fun updateView(newState: UserUiState, currentState: UserUiState) {
        if (newState.user != currentState.user) showUser(newState.user!!)
        if (newState.pagedListLiveData != currentState.pagedListLiveData) setImagesPagedList(newState.pagedListLiveData!!)
    }

    private fun setImagesPagedList(pagedListLiveData: LiveData<PagedList<String>>){
        val adapter = ImagesPagedAdapter(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        pagedListLiveData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.recyclerView.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }
    }

    private fun showUser(user: User) {
        binding.userStatus.text = user.status
        binding.userPhoto.transitionName = user.photo
        binding.userPhone.text = user.phone
        (activity as AppCompatActivity).supportActionBar?.title = user.name
        if (user.photo != null) {
            val bitmap = BitmapFactory.decodeFile(getFilePath(requireContext(), user.photo!!))
            binding.userPhoto.setImageBitmap(bitmap)
        } else {
            binding.userPhoto.setImageResource(R.drawable.user_icon)
        }
    }

    private fun setMainPhotoListener(navController: NavController){
        binding.userPhoto.setOnClickListener {
            val photo = viewModel.currentState.user?.photo
            if (photo != null) {
                val extras = FragmentNavigator.Extras.Builder()
                    .addSharedElement(binding.userPhoto, binding.userPhoto.transitionName)
                    .build()
                val bundle = Bundle().apply { putString("image", photo) }
                navController.navigate(R.id.imageViewFragment, bundle, null, extras)
            }
        }
    }
}
