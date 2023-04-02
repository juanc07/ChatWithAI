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
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.R
import com.thinkbloxph.chatwithai.TAG
import com.thinkbloxph.chatwithai.api.GoogleApi
import com.thinkbloxph.chatwithai.databinding.FragmentLoginScreenBinding
import com.thinkbloxph.chatwithai.helper.AudioRecorder
import com.thinkbloxph.chatwithai.helper.FirebaseHelper
import com.thinkbloxph.chatwithai.helper.RemoteConfigManager
import com.thinkbloxph.chatwithai.helper.UIHelper
import com.thinkbloxph.chatwithai.network.Provider
import com.thinkbloxph.chatwithai.network.UserDatabase
import com.thinkbloxph.chatwithai.network.model.User
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel
import java.util.*

private const val INNER_TAG = "LoginScreenFragment"
class LoginScreenFragment: Fragment() {
    private var _binding: FragmentLoginScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth
    private val firebaseHelper = FirebaseHelper()
    private var userDb: UserDatabase? = null
    private lateinit var callback: OnBackPressedCallback
    private var versionText: TextView? = null

    //private var facebookApi: FacebookApi? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginScreenBinding.inflate(inflater, container, false)
        versionText = binding.versionNameTextview

        // this is called once here because this is the 1st screen or fragment that will always be loaded 1st
        // after main activity
        FirebaseApp.initializeApp(requireContext())

        firebaseAuth = Firebase.auth
        firebaseAuth.setLanguageCode("en")

        fetchRemoteConfig()

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        /*facebookApi =
            this.activity?.let { FacebookApi(it.application, this.requireActivity(), this) }
        facebookApi?.init()*/

        this.activity?.let { GoogleApi.initInstance(it.application, this.requireActivity(), this) }
        GoogleApi.getInstance()?.init()

        if (AudioRecorder.getInstance() == null) {
            this.activity?.application?.let {
                AudioRecorder.initInstance(
                    it,
                    this.requireActivity(),
                    this
                )
            }
            AudioRecorder.getInstance()?.init()
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            userViewModel = _userViewModel
            loginScreenFragment = this@LoginScreenFragment
        }

        val packageManager = context?.packageManager
        val packageName = context?.packageName
        val packageInfo = packageName?.let { packageManager?.getPackageInfo(it, 0) }
        val versionName = packageInfo?.versionName
        versionText!!.text = "v:$versionName"

        versionName?.let { _userViewModel.setAppVersion(it) }
        val options = FirebaseOptions.fromResource(requireContext())
        options?.databaseUrl?.let { _userViewModel.setDefaultDBUrl(it) }
        Log.d(TAG, "default_db_url: ${_userViewModel.getDefaultDBUrl()}")
        userDb = UserDatabase(_userViewModel.getDefaultDBUrl())

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            UIHelper.getInstance().showLoading()
            getSetProvider()

            userDb!!.checkIfDataExists(currentUser.uid, callback = {
                    exists->
                if(exists){
                    loadUserData(currentUser.uid)
                    Log.d(TAG, "[${INNER_TAG}]: current user exist and user is in database")
                }else{
                    Log.d(TAG, "[${INNER_TAG}]: current user exist but user is not in database")
                    UIHelper.getInstance().hideLoading()
                }
            })
        }

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event here
                // For example, you can show a dialog or navigate to a different screen
                Log.v(TAG, "[${INNER_TAG}]: handleOnBackPressed event!!")
                requireActivity().finishAffinity()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(false)
    }

    private fun fetchRemoteConfig(){
        // Load the latest values from the server
        RemoteConfigManager.load { isSuccessful->
            if(isSuccessful){
                _userViewModel.setInitialFreeCredit(RemoteConfigManager.getLong("initial_free_credit"))
            }else{
                // default
                _userViewModel.setSearchNumResults(3)
                _userViewModel.setEnableSearch(true)
                _userViewModel.setCreditUsage(1)
                _userViewModel.setCompletionCreditPrice(1)
                _userViewModel.setRecordCreditPrice(5)
                _userViewModel.setInitialFreeCredit(10)
                _userViewModel.setGptModel("gpt-4")
            }
        }
    }

    /*fun continueWithFB() {
        facebookApi?.login(
            successCallback = { firebaseUser ->
                if (firebaseUser != null) {
                    var currentUser = firebaseAuth.currentUser;
                    if(currentUser!=null){
                        userDb.checkIfDataExists(currentUser.uid) { exists ->
                            if (exists) {
                                loadUserData(currentUser.uid)
                            } else {
                                setUserData(currentUser)
                                if(_userViewModel.getEmail().isNullOrEmpty()){
                                    facebookApi!!.email?.let { email-> _userViewModel.setEmail(email) }
                                }
                                if(_userViewModel.getPhoneNumber().isNullOrEmpty()){
                                    facebookApi!!.phoneNumber?.let { phoneNumber-> _userViewModel.setPhoneNumber(phoneNumber) }
                                }

                                val user = User(currentUser.uid, _userViewModel.getDisplayName(),
                                    _userViewModel.getEmail(), _userViewModel.getPhoneNumber(),
                                    _userViewModel.getGoogleUserId(), _userViewModel.getFacebookUserId(),
                                    _userViewModel.getCredit(), _userViewModel.getIsSubscribed()
                                    )
                                userDb.saveCurrentUserToDatabase(user) { isSuccess ->
                                    if (isSuccess) {
                                        UIHelper.getInstance().hideLoading()
                                        findNavController().navigate(R.id.action_loginScreenFragment_to_welcomeScreenFragment)
                                    }else{
                                        UIHelper.getInstance().hideLoading()
                                        UIHelper.getInstance().showDialogMessage(
                                            getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            failedCallback = {
                UIHelper.getInstance().hideLoading()
                UIHelper.getInstance().showDialogMessage(
                    getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
                )
                Log.d(TAG, "[${INNER_TAG}]: facebook Failed!")
            }
        )
    }*/

    fun continueWithGoogle() {
        UIHelper.getInstance().showLoading()
        GoogleApi.getInstance()?.login(successCallback = { googleSignInAccount, firebaseUser ->
            UIHelper.getInstance().hideLoading()
            Log.d(TAG, "[${INNER_TAG}]: LoginGoogle success!")

            if (firebaseUser != null) {
                var currentUser = firebaseAuth.currentUser;
                if(currentUser!=null){
                    userDb?.checkIfDataExists(currentUser.uid) { exists ->
                        if (exists) {
                            loadUserData(currentUser.uid)
                        } else {
                            setUserData(currentUser)

                            if(googleSignInAccount!=null) {
                                if(_userViewModel.getEmail().isNullOrEmpty()){
                                    googleSignInAccount.email?.let { email-> _userViewModel.setEmail(email) }
                                }
                            }

                            val user = User(currentUser.uid, _userViewModel.getDisplayName(),
                                _userViewModel.getEmail(), _userViewModel.getPhoneNumber(),
                                _userViewModel.getGoogleUserId(), _userViewModel.getFacebookUserId(),
                                _userViewModel.getCredit(), _userViewModel.getIsSubscribed()
                            )
                            userDb!!.saveCurrentUserToDatabase(user) { isSuccess ->
                                if (isSuccess) {
                                    UIHelper.getInstance().hideLoading()
                                    findNavController().navigate(R.id.action_loginScreenFragment_to_welcomeScreenFragment)
                                }else{
                                    UIHelper.getInstance().hideLoading()
                                    UIHelper.getInstance().showDialogMessage(
                                        getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "[${INNER_TAG}]: LoginGoogle success but firebaseUser is null")
            }
        }, failedCallback = {
            UIHelper.getInstance().hideLoading()
            UIHelper.getInstance().showDialogMessage(
                getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
            )
            Log.d(TAG, "[${INNER_TAG}]: loginGoogle Failed!")
        })
    }

    fun loadUserData(uid:String){
        userDb?.loadUserData(uid) { user ->
            if (user != null) {
                // Do something with the loaded user data
                user.googleId?.let { googleId-> _userViewModel.setGoogleUserId(googleId) }
                user.facebookId?.let { facebookId-> _userViewModel.setFacebookUserId(facebookId) }
                user.displayName?.let { displayName-> _userViewModel.setDisplayName(displayName) }
                user.phoneNumber?.let { phoneNumber-> _userViewModel.setPhoneNumber(phoneNumber) }
                user.email?.let { email-> _userViewModel.setEmail(email) }
                user.credit?.let { credit->_userViewModel.setCredit(credit)}
                Log.d(TAG, "[${INNER_TAG}]: 1st check isSubscribed: ${user.isSubscribed}!")
                user.isSubscribed?.let { isSubscribed-> _userViewModel.setIsSubscribed(isSubscribed) }
                val timestamp = user.timestamp
                Log.d(TAG, "User ${user.uid} was created at ${Date(timestamp)}")
                timestamp?.let { timestamp-> _userViewModel.setCreatedDate(timestamp) }

                Log.d(TAG, "[${INNER_TAG}]: check displayName: ${_userViewModel.getDisplayName()}!")
                Log.d(TAG, "[${INNER_TAG}]: check credit: ${_userViewModel.getCredit()}!")
                Log.d(TAG, "[${INNER_TAG}]: 2nd check isSubscribed: ${_userViewModel.getIsSubscribed()}!")
                Log.d(TAG, "[${INNER_TAG}]: check getCreatedDate: ${_userViewModel.getCreatedDate()}!")
                Log.d(TAG, "[${INNER_TAG}]: loadUserData success!")

                UIHelper.getInstance().hideLoading()
                try {

                    findNavController().navigate(R.id.action_loginScreenFragment_to_welcomeScreenFragment)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Navigation error: ${e.message}")
                    // Handle the error gracefully
                    UIHelper.getInstance().showDialogMessage(
                        getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
                    )
                }
            } else {
                // No data exists for the given UID
                UIHelper.getInstance().hideLoading()
                UIHelper.getInstance().showDialogMessage(
                    getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
                )
            }
        }
    }

    private fun setUserData(currentUser:FirebaseUser){
        if(currentUser!=null){
            currentUser.displayName?.let { displayName-> _userViewModel.setDisplayName(displayName) }
            currentUser.phoneNumber?.let { phoneNumber-> _userViewModel.setPhoneNumber(phoneNumber) }
            currentUser.email?.let { email-> _userViewModel.setEmail(email) }
            _userViewModel.setCredit(_userViewModel.getInitialFreeCredit()?.toInt())
            _userViewModel.setIsSubscribed(false)
            _userViewModel.setCreatedDate(System.currentTimeMillis())
            getSetProvider()
        }
    }

    private fun getSetProvider(){
        when (firebaseHelper.getProvider()) {
            Provider.FACEBOOK -> {
                var providerId = firebaseHelper.getProviderId(Provider.FACEBOOK)
                if (providerId != null) {
                    Log.d(TAG, "[${INNER_TAG}]:Facebook providerId: $providerId")
                    _userViewModel.setProviderId(providerId)
                    _userViewModel.setFacebookUserId(providerId)
                }
            }
            Provider.GOOGLE -> {
               var providerId = firebaseHelper.getProviderId(Provider.GOOGLE)
                if (providerId != null) {
                    Log.d(TAG, "[${INNER_TAG}]:Google providerId: $providerId")
                    _userViewModel.setProviderId(providerId)
                    _userViewModel.setGoogleUserId(providerId)
                }
            }
            Provider.PHONE -> {
                var providerId = firebaseHelper.getProviderId(Provider.PHONE)
                if (providerId != null) {
                    _userViewModel.setProviderId(providerId)
                    Log.d(TAG, "[${INNER_TAG}]:PHONE providerId: $providerId")
                }
            }
            else -> {
                var providerId = firebaseHelper.getProviderId(Provider.PASSWORD)
                if (providerId != null) {
                    _userViewModel.setProviderId(providerId)
                    Log.d(TAG, "[${INNER_TAG}]:PASSWORD providerId: $providerId")
                }
            }
        }
    }

    private fun extractGoogleSignInData(googleSignInAccount: GoogleSignInAccount){
        if(googleSignInAccount!=null){
            // Extract the user's data from the account object
            val email = googleSignInAccount?.email
            val displayName = googleSignInAccount?.displayName
            val idToken = googleSignInAccount?.idToken

            val givenName = googleSignInAccount?.givenName
            val familyName = googleSignInAccount?.familyName
            val personId = googleSignInAccount?.id
            val imageUrl = googleSignInAccount?.photoUrl

            Log.d(TAG, "[${INNER_TAG}]: check data pass in login screen!")
            Log.d(TAG, "Check idToken: ${idToken}")
            Log.d(TAG, "Check personId: ${personId}")
            Log.d(TAG, "Check Email: ${email}")
            Log.d(TAG, "Check displayName: ${displayName}")
            Log.d(TAG, "Check givenName: ${givenName}")
            Log.d(TAG, "Check familyName: ${familyName}")
            Log.d(TAG, "Check imageUrl: ${imageUrl}")
        }
    }

    fun goToChat() {
        findNavController().navigate(R.id.action_loginScreenFragment_to_welcomeScreenFragment)
    }
}