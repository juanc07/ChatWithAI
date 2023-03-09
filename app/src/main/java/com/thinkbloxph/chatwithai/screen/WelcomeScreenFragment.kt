package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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
        //creditText.text = getString(R.string.credit_remaining, _userViewModel.getCredit())

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callback.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(false)
        //UIHelper.getInstance().showHideActionBarWithoutBackButton(true,(requireActivity() as MainActivity).binding)

        UIHelper.getInstance().showHideActionBarWithoutBackButton(false,(requireActivity() as MainActivity).binding)
        showHideBottomNavigation(false)
        showHideSideNavigation(false)
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
                LoginManager.getInstance().logOut();
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
        Log.v(TAG, "[${INNER_TAG}]: buyCredit!")
        findNavController().navigate(R.id.action_welcomeScreenFragment_to_shopScreenFragment)

    }
}