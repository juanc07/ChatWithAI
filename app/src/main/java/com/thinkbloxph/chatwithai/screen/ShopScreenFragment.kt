package com.thinkbloxph.chatwithai.screen


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import com.thinkbloxph.chatwithai.constant.SkuConstants
import com.thinkbloxph.chatwithai.databinding.FragmentShopScreenBinding
import com.thinkbloxph.chatwithai.helper.InAppPurchaseManager
import com.thinkbloxph.chatwithai.helper.UIHelper
import com.thinkbloxph.chatwithai.network.UserDatabase
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel


private const val INNER_TAG = "ShopScreenFragment"
class ShopScreenFragment: Fragment() {
    private var _binding: FragmentShopScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var creditText:TextView
    private lateinit var subscriptionBtn:Button
    private lateinit var inAppPurchaseManager: InAppPurchaseManager
    private val userDb = UserDatabase()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentShopScreenBinding.inflate(inflater, container, false)
        firebaseAuth = Firebase.auth

        creditText = binding.creditText
        subscriptionBtn = binding.buySubscriptionBtn

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        inAppPurchaseManager = InAppPurchaseManager.getInstance(requireActivity())
        inAppPurchaseManager.setOnPurchasesUpdatedListener { purchases ->
            // Handle the list of purchases
            for (purchase in purchases) {
                // Handle the purchase here
                val productId = purchase.skus
                val purchaseToken = purchase.purchaseToken

                Log.v(TAG, "[${INNER_TAG}]: productId ${productId}}")
                Log.v(TAG, "[${INNER_TAG}]: purchaseToken ${purchaseToken}}")
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            userViewModel = _userViewModel
            shopScreenFragment = this@ShopScreenFragment
        }

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

        var isSubscribed = _userViewModel.getIsSubscribed()
        if (isSubscribed != null) {
            showHideUnlimited(isSubscribed)
        }
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

    fun showHideUnlimited(isSubscribed:Boolean){
        if(isSubscribed){
            creditText.text = getString(R.string.unlimited_credit)
            subscriptionBtn.visibility = View.GONE
            Log.d(TAG, "isSubscribed changed: $isSubscribed")
        }else{
            creditText.text = getString(R.string.credit_remaining, _userViewModel.getCredit())
            subscriptionBtn.visibility = View.VISIBLE
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

    private fun updateCredit(creditToAdd:Int){
        _userViewModel.getCredit()?.let { it1 ->
            userDb.updateCredit(it1,creditToAdd, callback = { newCredit, isSuccess->
                if(isSuccess){
                    if (newCredit != null) {
                        _userViewModel.setCredit(newCredit)
                    }
                    Log.d(TAG, "[${INNER_TAG}]: add ${creditToAdd} credit success!")
                    Log.d(TAG, "[${INNER_TAG}]: newCredit: ${newCredit}")
                }else{
                    Log.d(TAG, "[${INNER_TAG}]: add credit failed!")
                }
            })
        }
    }

    fun buyCreditTen(){
        Log.v(TAG, "[${INNER_TAG}]: buyCredit 10!")
        inAppPurchaseManager.purchaseInApp(SkuConstants.CWAI_CREDIT_10){ sku,isSuccess ->
            if (!isSuccess) {
                UIHelper.getInstance().showDialogMessage(
                    getString(R.string.purchase_failed), getString(R.string.dialog_ok)
                )
            }else{
                if(sku.equals(SkuConstants.CWAI_CREDIT_10)){
                    Log.v(TAG, "[${INNER_TAG}]: purchase 10 credit success!")
                    updateCredit(10)
                }
            }
        }
    }

    fun buyCreditTwenty(){
        Log.v(TAG, "[${INNER_TAG}]: buyCredit 20!")
        inAppPurchaseManager.purchaseInApp(SkuConstants.CWAI_CREDIT_20){ sku,isSuccess ->
            if (!isSuccess) {
                UIHelper.getInstance().showDialogMessage(
                    getString(R.string.purchase_failed), getString(R.string.dialog_ok)
                )
            }else{
                if(sku.equals(SkuConstants.CWAI_CREDIT_20)){
                    Log.v(TAG, "[${INNER_TAG}]: purchase 20 credit success!")
                    updateCredit(20)
                }
            }
        }
    }

    fun buyCreditFifty(){
        Log.v(TAG, "[${INNER_TAG}]: buyCredit 50!")
        inAppPurchaseManager.purchaseInApp(SkuConstants.CWAI_CREDIT_50){ sku,isSuccess ->
            if (!isSuccess) {
                UIHelper.getInstance().showDialogMessage(
                    getString(R.string.purchase_failed), getString(R.string.dialog_ok)
                )
            }else{
                if(sku.equals(SkuConstants.CWAI_CREDIT_50)){
                    Log.v(TAG, "[${INNER_TAG}]: purchase 50 credit success!")
                    updateCredit(50)
                }
            }
        }
    }

    fun buyCreditOneHundred(){
        Log.v(TAG, "[${INNER_TAG}]: buyCredit 100!")
        inAppPurchaseManager.purchaseInApp(SkuConstants.CWAI_CREDIT_100){ sku,isSuccess ->
            if (!isSuccess) {
                UIHelper.getInstance().showDialogMessage(
                    getString(R.string.purchase_failed), getString(R.string.dialog_ok)
                )
            }else{
                if(sku.equals(SkuConstants.CWAI_CREDIT_100)){
                    Log.v(TAG, "[${INNER_TAG}]: purchase 100 credit success!")
                    updateCredit(100)
                }
            }
        }
    }

    fun buyMonthlySubscription(){
        //if(!inAppPurchaseManager.isSubscribed(SkuConstants.SubscriptionSku)){
        if(!_userViewModel.getIsSubscribed()!!){
            Log.v(TAG, "[${INNER_TAG}]: buy Monthly Subscription ")
            inAppPurchaseManager.purchaseSubscription(SkuConstants.SubscriptionSku){ sku,isSuccess ->
                if (!isSuccess) {
                    UIHelper.getInstance().showDialogMessage(
                        getString(R.string.something_went_wrong), getString(R.string.dialog_ok)
                    )
                }else{
                    if(sku == SkuConstants.SubscriptionSku){
                        Log.v(TAG, "[${INNER_TAG}]: purchase subscription unli credit success!")
                        userDb.updateSubscription(true)
                        _userViewModel.setIsSubscribed(true)
                    }
                }
            }
        }else{
            Log.v(TAG, "[${INNER_TAG}]: already subscribed")
            // unsubscribe action
            inAppPurchaseManager.unSubscribe(SkuConstants.SubscriptionSku){ sku,isSuccess ->
                if (isSuccess) {
                    if(sku == SkuConstants.SubscriptionSku){
                        Log.v(TAG, "[${INNER_TAG}]: unSubscribe unli credit success!")
                        userDb.updateSubscription(false)
                        _userViewModel.setIsSubscribed(false)
                    }
                }else{
                    Log.v(TAG, "[${INNER_TAG}]: unSubscribe unli credit failed!")
                }
            }
        }
    }
}