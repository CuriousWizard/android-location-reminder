package com.curiouswizard.locationreminder.locationreminders.reminderslist

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import com.curiouswizard.locationreminder.R
import com.curiouswizard.locationreminder.authentication.AuthenticationActivity
import com.curiouswizard.locationreminder.base.BaseFragment
import com.curiouswizard.locationreminder.base.NavigationCommand
import com.curiouswizard.locationreminder.databinding.FragmentRemindersBinding
import com.curiouswizard.locationreminder.locationreminders.ReminderDescriptionActivity
import com.curiouswizard.locationreminder.utils.setDisplayHomeAsUpEnabled
import com.curiouswizard.locationreminder.utils.setTitle
import com.curiouswizard.locationreminder.utils.setup
import com.firebase.ui.auth.AuthUI
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
    }

    override fun onResume() {
        super.onResume()
        // Load the reminders list on the UI
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        // Use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter { item ->
            val intent = requireActivity().run {
                ReminderDescriptionActivity.newIntent(this, item)
            }
            startActivity(intent)
        }

        // Setup the recycler view using the extension function
        binding.remindersRecyclerView.setup(adapter)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Logout user if clicked to that menu item
            R.id.logout -> {
                AuthUI.getInstance()
                    .signOut(requireContext())
                    .addOnCompleteListener {
                        val loginIntent = Intent(context, AuthenticationActivity::class.java)
                        startActivity(loginIntent)
                        requireActivity().finish()
                    }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

}
