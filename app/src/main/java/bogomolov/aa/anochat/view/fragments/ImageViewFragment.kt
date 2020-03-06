package bogomolov.aa.anochat.view.fragments

import android.graphics.BitmapFactory
import androidx.transition.TransitionInflater
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.android.getFilesDir
import bogomolov.aa.anochat.databinding.FragmentImageViewBinding
import bogomolov.aa.anochat.view.MainActivity
import java.io.File


class ImageViewFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        sharedElementReturnTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentImageViewBinding>(
            inflater,
            R.layout.fragment_image_view,
            container,
            false
        )
        val mainActivity = activity as MainActivity
        mainActivity.setSupportActionBar(binding.toolbar)
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)
        mainActivity.supportActionBar?.title = ""


        var expanded = false
        val imageName = arguments?.getString("image")!!
        val imagePath = File(getFilesDir(requireContext()), imageName).path
        binding.imageView.transitionName = imageName
        binding.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        binding.imageView.setOnClickListener {
            binding.toolbar.visibility = if (expanded) View.INVISIBLE else View.VISIBLE
            expanded = !expanded
        }

        return binding.root
    }

}
