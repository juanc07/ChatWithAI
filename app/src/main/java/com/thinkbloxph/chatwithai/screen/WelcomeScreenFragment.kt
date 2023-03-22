package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.facebook.login.LoginManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.MainActivity
import com.thinkbloxph.chatwithai.R
import com.thinkbloxph.chatwithai.TAG
import com.thinkbloxph.chatwithai.api.GoogleApi
import com.thinkbloxph.chatwithai.databinding.FragmentWelcomeScreenBinding
import com.thinkbloxph.chatwithai.helper.InAppPurchaseManager
import com.thinkbloxph.chatwithai.helper.RemoteConfigManager
import com.thinkbloxph.chatwithai.helper.UIHelper
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel


private const val INNER_TAG = "WelcomeScreenFragment"
class WelcomeScreenFragment: Fragment() {
    private var _binding: FragmentWelcomeScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var welcomeText:TextView
    private lateinit var creditText:TextView
    private lateinit var callback: OnBackPressedCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWelcomeScreenBinding.inflate(inflater, container, false)
        welcomeText = binding.welcomebackText
        creditText = binding.creditText

        firebaseAuth = Firebase.auth

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        // init shop
        InAppPurchaseManager.getInstance(requireActivity())

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            userViewModel = _userViewModel
            welcomeScreenFragment = this@WelcomeScreenFragment
        }

        Log.d(TAG, "[${INNER_TAG}]: check displayname: ${_userViewModel.getDisplayName()}}!")
        welcomeText.text = getString(R.string.welcome, _userViewModel.getDisplayName())

        _userViewModel.credit.observe(viewLifecycleOwner, Observer { credit ->
            // Do something with the new credit value
            creditText.text = getString(R.string.credit_remaining, _userViewModel.getCredit())
            Log.d(TAG, "Credit changed: $credit")
        })

        _userViewModel.isSubscribed.observe(viewLifecycleOwner, Observer { isSubscribed ->
            // Do something with the new credit value
            if(isSubscribed){
                showHideUnlimited(isSubscribed)
                Log.d(TAG, "isSubscribed changed: $isSubscribed")
            }else{
                creditText.text = getString(R.string.credit_remaining, _userViewModel.getCredit())
            }
        })

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event here
                // For example, you can show a dialog or navigate to a different screen
                Log.v(TAG, "[${INNER_TAG}]: handleOnBackPressed event!!")
                signout()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        fetchRemoteConfig()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed() // Call onBackPressed() to handle the back button press in the fragment
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callback.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        //UIHelper.getInstance().showHideActionBarWithoutBackButton(true,(requireActivity() as MainActivity).binding)
        UIHelper.getInstance().showHideActionBar(true,(requireActivity() as MainActivity).binding)
        showHideBottomNavigation(false)
        showHideSideNavigation(false)
    }

    private fun fetchRemoteConfig(){
        // Load the latest values from the server
        RemoteConfigManager.load { isSuccessful->
            if(isSuccessful){
                // Retrieve the value of a parameter
                val welcomeMessage = RemoteConfigManager.getString("welcome_message")
                // Use the retrieved value as needed
                Log.d(TAG, "Welcome message: $welcomeMessage")

                // Fetch the value of the "app_version" parameter
                val appVersion = RemoteConfigManager.getString("app_version")
                Log.d(TAG, "appVersion: $appVersion")

                // Fetch the value of the "search_num_results" parameter
                _userViewModel.setSearchNumResults(RemoteConfigManager.getLong("search_num_results"))
                Log.d(TAG, "searchNumResults: ${_userViewModel.getSearchNumResults()}")

                _userViewModel.setEnableSearch(RemoteConfigManager.getBoolean("enable_search"))
                Log.d(TAG, "enableSearch: ${_userViewModel.getEnableSearch()}")
            }else{
                // default
                _userViewModel.setSearchNumResults(3)
                Log.d(TAG, "searchNumResults: ${_userViewModel.getSearchNumResults()}")

                _userViewModel.setEnableSearch(true)
                Log.d(TAG, "enableSearch: ${_userViewModel.getEnableSearch()}")
            }
        }
    }

    fun showHideUnlimited(isSubscribed:Boolean){
        if(isSubscribed){
            creditText.text = getString(R.string.unlimited_credit)
            Log.d(TAG, "isSubscribed changed: $isSubscribed")
        }else{
            creditText.text = getString(R.string.credit_remaining, _userViewModel.getCredit())
        }
    }

    private fun showHideBottomNavigation(isShow: Boolean) {
        val bottomNav =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        if (isShow) {
            bottomNav?.visibility = View.VISIBLE
        } else {
            bottomNav?.visibility = View.GONE
        }
    }

    private fun showHideSideNavigation(isShow: Boolean) {
        val sideNavView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        if (isShow) {
            sideNavView?.visibility = View.VISIBLE
        } else {
            sideNavView?.visibility = View.GONE
        }
    }

    fun goToLogin() {
        findNavController().navigate(R.id.action_welcomeScreenFragment_to_chatScreenFragment)
    }

    fun signout(){
        var currentProvider: String? = null
        if (firebaseAuth.currentUser?.providerData?.size!! > 0) {
            //Prints Out google.com for Google Sign In, prints facebook.com for Facebook
            currentProvider =  firebaseAuth.currentUser!!.providerData.get(firebaseAuth.currentUser!!.providerData.size - 1).providerId
            Log.v(TAG, "[${INNER_TAG}]: currentProvider ${currentProvider}")
        }

        when(currentProvider){
            "facebook.com" -> {
                //LoginManager.getInstance().logOut();
                Log.v(TAG, "[${INNER_TAG}]: facebook SignOut success!")
            }
            "google.com" -> {
                GoogleApi.getInstance()?.signOut {
                    Log.v(TAG, "[${INNER_TAG}]: google SignOut success!")
                }
            }
            else ->{
                Log.v(TAG, "[${INNER_TAG}]: provider not detected!")
            }
        }
        firebaseAuth.signOut()
        findNavController().navigate(R.id.action_welcomeScreenFragment_to_loginScreenFragment)
    }

    fun buyCredit(){
        findNavController().navigate(R.id.action_welcomeScreenFragment_to_shopScreenFragment)
    }
}