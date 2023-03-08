package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.R
import com.thinkbloxph.chatwithai.TAG
import com.thinkbloxph.chatwithai.api.GoogleApi
import com.thinkbloxph.chatwithai.databinding.FragmentLoginScreenBinding
import com.thinkbloxph.chatwithai.helper.FirebaseHelper
import com.thinkbloxph.chatwithai.helper.UIHelper
import com.thinkbloxph.chatwithai.network.Provider
import com.thinkbloxph.chatwithai.network.UserDatabase
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel

private const val INNER_TAG = "LoginScreenFragment"
class LoginScreenFragment: Fragment() {
    private var _binding: FragmentLoginScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth
    private val firebaseHelper = FirebaseHelper()
    private val userDb = UserDatabase()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginScreenBinding.inflate(inflater, container, false)

        // this is called once here because this is the 1st screen or fragment that will always be loaded 1st
        // after main activity
        FirebaseApp.initializeApp(requireContext())

        firebaseAuth = Firebase.auth
        firebaseAuth.setLanguageCode("en")

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        this.activity?.let { GoogleApi.initInstance(it.application, this.requireActivity(), this) }
        GoogleApi.getInstance()?.init()

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

        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            UIHelper.getInstance().showLoading()
            var currentProvider: String? = null
            if (currentUser.providerData?.size!! > 0) {
                currentProvider =
                    currentUser.providerData.get(currentUser.providerData.size - 1).providerId
                Log.v(TAG, "[${INNER_TAG}]: currentProvider ${currentProvider}")
            }

            if (currentProvider != null) {
                Log.v(TAG, "[${INNER_TAG}]: currentProvider ${currentProvider}")

                val providerId = firebaseHelper.getProviderId(currentProvider)
                if (!providerId.isNullOrEmpty()) {
                    _userViewModel.setProviderId(providerId)
                    Log.d(TAG, "[${INNER_TAG}]: providerId is set successfully")

                    /*firebaseHelper.getIdToken(
                        successCallback = { mobileNumber, idToken, email ->
                            if (email != null && !idToken.isNullOrEmpty()) {
                                Log.d(TAG, "[${INNER_TAG}]: Auto External Login mobileNumber: $mobileNumber")
                                Log.d(TAG, "[${INNER_TAG}]: Auto External Login email: $email")
                                Log.d(TAG, "[${INNER_TAG}]: Auto External Login idToken : $idToken")

                            }
                        }, failedCallback = {
                            // show some dialog here login failed
                            Log.d(TAG, "[${INNER_TAG}]: Auto External get initial IdToken failed")
                            UIHelper.getInstance().hideLoading()
                        })*/

                    Log.d(TAG, "[${INNER_TAG}]: check uid: ${currentUser.uid}")

                    userDb.checkIfDataExists(currentUser.uid, callback = {
                        exists->
                            if(exists){
                                loadUserData(currentUser.uid)
                                Log.d(TAG, "[${INNER_TAG}]: current user exist and user is in database")
                            }else{
                                Log.d(TAG, "[${INNER_TAG}]: current user exist but user is not in database")

                        }
                    })

                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(false)
    }

    fun continueWithGoogle() {
        var providerId: String? = null
        UIHelper.getInstance().showLoading()
        GoogleApi.getInstance()?.login(successCallback = { googleSignInAccount, firebaseUser ->
            UIHelper.getInstance().hideLoading()

            Log.d(TAG, "[${INNER_TAG}]: LoginGoogle success!")

            if (firebaseUser != null) {
                providerId = firebaseHelper.getProviderId(Provider.GOOGLE)
                if (providerId != null) {
                    Log.d(TAG, "[${INNER_TAG}]:Google ID: $providerId")
                }
                GoogleApi.getInstance().getUserInfo(firebaseUser)
            } else {
                Log.d(TAG, "[${INNER_TAG}]: LoginGoogle success but firebaseUser is null")
            }

            if (googleSignInAccount != null) {
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

                var currentUser = firebaseAuth.currentUser;
                if(currentUser!=null){

                    userDb.checkIfDataExists(currentUser.uid, callback = {
                            exists->
                        if(exists){
                            loadUserData(currentUser.uid)
                        }else{
                            providerId?.let { userDb.saveCurrentUserToDatabase(it) }
                            // fix this save method from userDB use callback
                            loadUserData(currentUser.uid)
                        }
                    })
                }
            } else {
                Log.d(TAG, "[${INNER_TAG}]: LoginGoogle success but googleSignInAccount is null")
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
        userDb.loadUserData(uid) { user ->
            if (user != null) {
                // Do something with the loaded user data
                user.googleId?.let { googleId-> _userViewModel.setGoogleUserId(googleId) }
                user.facebookId?.let { facebookId-> _userViewModel.setFacebookUserId(facebookId) }
                user.displayName?.let { displayName-> _userViewModel.setDisplayName(displayName) }
                user.phoneNumber?.let { phoneNumber-> _userViewModel.setPhoneNumber(phoneNumber) }
                user.email?.let { email-> _userViewModel.setEmail(email) }
                user.credit?.let { credit->_userViewModel.setCredit(credit)}
                user.isSubscribed?.let { isSubscribed-> _userViewModel.setIsSubscribed(isSubscribed) }
                user.createdDate?.let { createdDate-> _userViewModel.setCreatedDate(createdDate) }

                Log.d(TAG, "[${INNER_TAG}]: check displayName: ${_userViewModel.getDisplayName()}}!")
                Log.d(TAG, "[${INNER_TAG}]: check credit: ${_userViewModel.getCredit()}}!")
                Log.d(TAG, "[${INNER_TAG}]: loadUserData success!")

                UIHelper.getInstance().hideLoading()
                findNavController().navigate(R.id.action_loginScreenFragment_to_welcomeScreenFragment)
            } else {
                // No data exists for the given UID
                UIHelper.getInstance().hideLoading()
                UIHelper.getInstance().showDialogMessage(
                    getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
                )
            }
        }
    }

    fun goToChat() {
        findNavController().navigate(R.id.action_loginScreenFragment_to_welcomeScreenFragment)
    }
}