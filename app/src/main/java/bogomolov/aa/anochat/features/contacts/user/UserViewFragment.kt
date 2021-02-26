package bogomolov.aa.anochat.features.contacts.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.NavigationUI
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.Transition
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentUserViewBinding
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.getBitmap
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import com.google.android.material.card.MaterialCardView
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class UserViewFragment : Fragment(), UpdatableView<UserUiState> {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: UserViewViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentUserViewBinding
    private lateinit var navController: NavController
    private lateinit var transition: Transition

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

        transition = Fade().apply { duration = 375 }
        transition.addTarget(R.id.toolbar)
        transition.addTarget(R.id.user_info)
        //transition.addTarget(R.id.userPhoto)

        exitTransition = transition
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserViewBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        setupImagesRecyclerView {
            startPostponedEnterTransition()
        }
        postponeEnterTransition()
        setPhotoClickListener(navController)

        return binding.root
    }

    override fun updateView(newState: UserUiState, currentState: UserUiState) {
        if (newState.user != currentState.user) showUser(newState.user!!)
        if (newState.pagedListLiveData != currentState.pagedListLiveData)
            setImagesPagedList(newState.pagedListLiveData!!)
    }

    private fun setImagesPagedList(pagedListLiveData: LiveData<PagedList<String>>) {
        pagedListLiveData.observe(viewLifecycleOwner) {
            (binding.recyclerView.adapter as ImagesPagedAdapter).submitList(it)
        }
    }

    private fun showUser(user: User) {
        binding.userStatus.text = user.status
        binding.userPhoto.transitionName = user.photo
        binding.userPhone.text = user.phone
        (activity as AppCompatActivity).supportActionBar?.title = user.name
        if (user.photo != null) {
            val bitmap = getBitmap(user.photo, requireContext())
            binding.userPhoto.setImageBitmap(bitmap)
        } else {
            binding.userPhoto.setImageResource(R.drawable.user_icon)
        }
    }

    private fun setupImagesRecyclerView(onPreDraw: () -> Unit) {
        val adapter = ImagesPagedAdapter { image, view ->
            val cardView = view as MaterialCardView
            val imageView = cardView.findViewById(R.id.image) as ImageView
            val extras =
                FragmentNavigator.Extras.Builder().addSharedElement(imageView, image).build()
            transition.removeTarget(R.id.userPhoto)
            transition.addTarget(R.id.userPhoto)
            navController.navigate(
                R.id.imageViewFragment,
                Bundle().apply {
                    putString("image", image)
                    putInt("quality", 8)
                },
                null,
                extras
            )
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.doOnPreDraw { onPreDraw() }
    }

    private fun setPhotoClickListener(navController: NavController) {
        binding.userPhoto.setOnClickListener {
            val photo = viewModel.state.user?.photo
            transition.removeTarget(R.id.userPhoto)
            if (photo != null) {
                val extras = FragmentNavigator.Extras.Builder()
                    .addSharedElement(binding.userPhoto, photo)
                    .build()
                val bundle = Bundle().apply {
                    putString("image", photo)
                    putInt("quality", 1)
                }
                navController.navigate(R.id.imageViewFragment, bundle, null, extras)
            }
        }
    }
}