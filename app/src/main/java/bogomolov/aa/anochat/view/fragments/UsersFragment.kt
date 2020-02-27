package bogomolov.aa.anochat.view.fragments


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.dagger.ViewModelFactory
import bogomolov.aa.anochat.databinding.FragmentUsersBinding
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class UsersFragment : Fragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    //search: https://stackoverflow.com/questions/34291453/adding-searchview-in-fragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentUsersBinding>(inflater,R.layout.fragment_users,container,false)
        val view = binding.root

        val navController = Navigation.findNavController(activity!!, R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.toolbar, navController)

        return view
    }


}
