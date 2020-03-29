package bogomolov.aa.anochat.view.fragments

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilePath
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentUserViewBinding
import bogomolov.aa.anochat.view.adapters.AdapterHelper
import bogomolov.aa.anochat.view.adapters.ImagesPagedAdapter
import bogomolov.aa.anochat.viewmodel.UserViewViewModel
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_user_view.*
import javax.inject.Inject

class UserViewFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    val viewModel: UserViewViewModel by activityViewModels { viewModelFactory }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentUserViewBinding>(
            inflater,
            R.layout.fragment_user_view,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val userId = arguments?.getLong("id")!!
        viewModel.loadUser(userId)
        viewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            (activity as AppCompatActivity).supportActionBar?.title = user.name
            if (user.photo != null) {
                val bitmap = BitmapFactory.decodeFile(getFilePath(requireContext(), user.photo!!))
                binding.userPhoto.setImageBitmap(bitmap)
            } else {
                binding.userPhoto.setImageResource(R.drawable.user_icon)
            }
        }
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        val adapter = ImagesPagedAdapter(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        viewModel.loadImages(userId).observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.recyclerView.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

        binding.userPhoto.setOnClickListener {
            val photo = viewModel.userLiveData.value?.photo
            if (photo != null) {
                val extras = FragmentNavigator.Extras.Builder()
                    .addSharedElement(binding.userPhoto, binding.userPhoto.transitionName)
                    .build()
                val bundle = Bundle().apply { putString("image", photo) }
                navController.navigate(R.id.imageViewFragment, bundle, null, extras)
            }
        }

        postponeEnterTransition()


        return binding.root
    }

}
