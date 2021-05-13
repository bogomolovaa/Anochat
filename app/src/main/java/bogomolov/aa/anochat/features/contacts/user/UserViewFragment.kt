package bogomolov.aa.anochat.features.contacts.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.Transition
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.databinding.FragmentUserViewBinding
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.shared.getBitmap
import bogomolov.aa.anochat.features.shared.mvi.StateLifecycleObserver
import bogomolov.aa.anochat.features.shared.mvi.UpdatableView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserViewFragment : Fragment(), UpdatableView<UserUiState> {
    val viewModel: UserViewViewModel by viewModels()
    private lateinit var binding: FragmentUserViewBinding
    private lateinit var navController: NavController
    private lateinit var transition: Transition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = arguments?.getLong("id")!!
        viewModel.addAction(InitUserAction(userId))

        val animationDuration = resources.getInteger(R.integer.animation_duration).toLong()
        transition = Fade().apply { duration = animationDuration }
        transition.addTarget(R.id.toolbar)
        transition.addTarget(R.id.user_info)
        exitTransition = transition
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentUserViewBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(StateLifecycleObserver(this, viewModel))
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        navController = findNavController()
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        setupImagesRecyclerView {
            startPostponedEnterTransition()
        }
        postponeEnterTransition()
        setPhotoClickListener(navController)
    }

    override fun updateView(newState: UserUiState, currentState: UserUiState) {
        if (newState.user != currentState.user) showUser(newState.user!!)
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
        val adapter = ImagesPagedAdapter {
            transition.removeTarget(R.id.userPhoto)
            transition.addTarget(R.id.userPhoto)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.doOnPreDraw { onPreDraw() }
        viewModel.imagesLiveData.observe(viewLifecycleOwner) {
            adapter.submitData(lifecycle, it)
        }
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