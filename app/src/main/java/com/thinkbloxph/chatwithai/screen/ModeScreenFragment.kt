package com.thinkbloxph.chatwithai.screen

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.MainActivity
import com.thinkbloxph.chatwithai.R
import com.thinkbloxph.chatwithai.TAG
import com.thinkbloxph.chatwithai.databinding.FragmentModeScreenBinding
import com.thinkbloxph.chatwithai.helper.UIHelper
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel

private const val INNER_TAG = "ModeScreenFragment"
class ModeScreenFragment: Fragment() {
    private var _binding: FragmentModeScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var callback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentModeScreenBinding.inflate(inflater, container, false)

        firebaseAuth = Firebase.auth

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            userViewModel = _userViewModel
            modeScreenFragment = this@ModeScreenFragment
        }

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event here
                // For example, you can show a dialog or navigate to a different screen
                Log.v(TAG, "[${INNER_TAG}]: handleOnBackPressed event!!")
                findNavController().navigate(R.id.action_modeScreenFragment_to_welcomeScreenFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        // Get the NavController for this fragment's NavHostFragment
        /*val navController = findNavController()

        // Get the current AppBarConfiguration
        val appBarConfig = AppBarConfiguration(navController.graph)

        // Create a new AppBarConfiguration with the new label
        val newAppBarConfig = appBarConfig.toBuilder()
            .setTopLevelDestination(R.id.my_destination)
            .setTitle(R.string.new_title)
            .build()

        // Set the new AppBarConfiguration on the NavController
        navController.setAppBarConfiguration(newAppBarConfig)*/
        //UIHelper.getInstance().setActionBarTitle("test2")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.v(TAG, "[${INNER_TAG}]: click back button")
                requireActivity().onBackPressed() // Call onBackPressed() to handle the back button press in the fragment
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(false)
    }

    fun goToChat() {
        findNavController().navigate(R.id.action_modeScreenFragment_to_chatScreenFragment)
    }

    fun selectPromptMode(mode:String) {
        _userViewModel.setCurrentPrompt(mode)
        findNavController().navigate(R.id.action_modeScreenFragment_to_chatScreenFragment)
    }
}