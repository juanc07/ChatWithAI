package com.thinkbloxph.chatwithai.helper

import android.app.Activity
import android.util.Log
import androidx.annotation.UiThread
import com.android.billingclient.api.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.thinkbloxph.chatwithai.constant.SkuConstants

class InAppPurchaseManager private constructor(private val activity: Activity) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private lateinit var skuDetailsMap: Map<String, SkuDetails>
    private lateinit var skuSubscriptionDetailsMap: Map<String, SkuDetails>
    private var purchasesUpdatedListener: ((List<Purchase>) -> Unit)? = null
    private var skuInAppDetailsFetched = false
    private var skuSubscriptionDetailsFetched = false
    var successfulPurchaseCallback: (String, Boolean) -> Unit = { _, _->}
    companion object {
        private const val TAG = "InAppPurchaseManager"
        @Volatile
        private var instance: InAppPurchaseManager? = null

        fun getInstance(activity: Activity): InAppPurchaseManager {
            return instance ?: synchronized(this) {
                instance ?: InAppPurchaseManager(activity).also {
                    instance = it
                }
            }
        }
    }

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected.")
                    querySkuDetails()
                    querySkuSubscriptionDetails()
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing client connection failed.")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing client disconnected.")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun querySkuDetails() {
        val skuList = listOf(SkuConstants.CWAI_CREDIT_10,SkuConstants.CWAI_CREDIT_20,SkuConstants.CWAI_CREDIT_50,SkuConstants.CWAI_CREDIT_100)
        val params = SkuDetailsParams.newBuilder()
            .setType(BillingClient.SkuType.INAPP)
            .setSkusList(skuList)
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "===============================[Shop Data(InApp)]============================================")
                Log.d(TAG, "Sku details query succeeded.")

                skuDetailsMap = skuDetailsList!!.associateBy { it.sku }

                skuList.forEach { sku ->
                    skuDetailsMap[sku]?.let { skuDetails ->
                        Log.d(TAG, "Product SKU: ${skuDetails.sku}")
                        Log.d(TAG, "Product title: ${skuDetails.title}")
                        Log.d(TAG, "Product description: ${skuDetails.description}")
                        Log.d(TAG, "Product price: ${skuDetails.price}")
                    } ?: Log.e(TAG, "Product SKU $sku not found in SKU details list.")
                }

                skuInAppDetailsFetched = true
                Log.d(TAG, "===============================[Shop Data(InApp)]============================================")
            } else {
                Log.e(TAG, "Sku details query failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun querySkuSubscriptionDetails() {
        val skuDetailsParams = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(SkuConstants.SubscriptionSku, SkuConstants.CWAI_UNLI_CREDIT_MONTHLY))
            .setType(BillingClient.SkuType.SUBS)
            .build()
        billingClient.querySkuDetailsAsync(skuDetailsParams) { billingResult, skuDetailsList ->
            if (skuDetailsList != null) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList.isNotEmpty()) {
                    if (skuDetailsList != null) {
                        skuSubscriptionDetailsMap = skuDetailsList.associateBy { it.sku }
                    }
                    val subscriptionSkuDetails = skuSubscriptionDetailsMap[SkuConstants.SubscriptionSku]
                    val basePlanSkuDetails = skuSubscriptionDetailsMap[SkuConstants.CWAI_UNLI_CREDIT_MONTHLY]
                    // Do something with the subscription and base plan SKU details

                    Log.d(TAG, "===============================[Shop Data Subscription]============================================")
                    Log.d(TAG, "subscriptionSkuDetails: ${subscriptionSkuDetails}")
                    Log.d(TAG, "basePlanSkuDetails: ${basePlanSkuDetails}")

                    Log.d(TAG, "Sku details querySkuSubscriptionDetails succeeded.")
                    skuSubscriptionDetailsFetched = true
                    Log.d(TAG, "===============================[Shop Data Subscription]============================================")
                } else {
                    // Handle the error
                    Log.e(TAG, "Sku details querySkuSubscriptionDetails failed: ${billingResult.debugMessage}")
                }
            }
        }
    }

    private fun queryPurchases() {
        val purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        if (purchasesResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Purchases query succeeded.")
            purchasesResult.purchasesList?.let { purchasesUpdatedListener?.invoke(it) }
        } else {
            //Log.e(TAG, "Purchases query failed: ${purchasesResult.debugMessage}")
        }
    }

    fun purchaseInApp(sku: String,successCallback: (String, Boolean) -> Unit) {
        successfulPurchaseCallback = successCallback
        if (!skuInAppDetailsFetched) {
            Log.e(TAG, "SKU details not fetched yet. Please wait for the query to complete.")
            successfulPurchaseCallback(sku,false)
            return
        }

        val skuDetails = skuDetailsMap[sku] ?: run {
            Log.e(TAG, "Sku details not found for $sku.")
            successfulPurchaseCallback(sku,false)
            return
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Purchase flow failed: ${responseCodeToMessage(responseCode)}")
        }
    }

    fun purchaseSubscription(sku: String,successCallback: (String, Boolean) -> Unit) {
        successfulPurchaseCallback = successCallback
        if (!skuSubscriptionDetailsFetched) {
            Log.e(TAG, "SKU susbcription details not fetched yet. Please wait for the query to complete.")
            successfulPurchaseCallback(sku,false)
            return
        }

        val skuDetails = skuSubscriptionDetailsMap[sku] ?: run {
            Log.e(TAG, "skuSubscriptionDetailsMap not found for $sku.")
            successfulPurchaseCallback(sku,false)
            return
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Purchase flow failed: ${responseCodeToMessage(responseCode)}")
        }
    }

    fun unSubscribe(sku: String,callback: (isSuccess:Boolean)->Unit) {
        val skuList = listOf(sku) // replace with your own SKU
        val subsParams = SkuDetailsParams.newBuilder()
            .setType(BillingClient.SkuType.SUBS)
            .setSkusList(skuList)
            .build()

        billingClient.querySkuDetailsAsync(subsParams) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val skuDetails = skuDetailsList?.firstOrNull()
                if (skuDetails != null) {
                    Log.v(TAG, " unSubscribe skuDetails: ${skuDetails}")
                    billingClient.launchPriceChangeConfirmationFlow(activity, PriceChangeFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build()) { billingResult ->

                        when (billingResult.responseCode) {
                            BillingClient.BillingResponseCode.OK -> {
                                Log.d(TAG, "unSubscribe Subscription canceled successfully")
                                // Update UI or perform any necessary actions
                            }
                            BillingClient.BillingResponseCode.USER_CANCELED -> {
                                Log.d(TAG, "unSubscribe User canceled the subscription cancelation")
                            }
                            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                                Log.d(TAG, "unSubscribe Billing service unavailable. Try again later")
                            }
                            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                                Log.d(TAG, "unSubscribe The subscription is not available for purchase")
                            }
                            BillingClient.BillingResponseCode.ERROR -> {
                                Log.d(TAG, "unSubscribe An error occurred while canceling the subscription: ${billingResult.debugMessage}")
                            }
                        }

                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            // User has successfully unsubscribed
                            Log.v(TAG, " unSubscribe skuDetails: ${skuDetails} success!!")
                            callback(true)
                        } else {
                            // Unsubscribe failed
                            Log.e(TAG, "unSubscribe Failed to acknowledge purchase: ${billingResult.debugMessage}")
                            callback(false)
                        }
                    }
                }
            }
        }
    }

    fun isSubscribed(sku: String): Boolean {
        val purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        val purchases = purchasesResult.purchasesList
        purchases?.let {
            for (purchase in it) {
                if (purchase.skus.contains(sku) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    return true
                }
            }
        }
        return false
    }


    fun setOnPurchasesUpdatedListener(listener: (List<Purchase>) -> Unit) {
        purchasesUpdatedListener = listener
    }

    /*@UiThread
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Purchases updated.")
            purchases?.let { purchasesUpdatedListener?.invoke(it) }
        } else {
            Log.e(
                TAG,
                "Purchases update failed: ${billingResult.debugMessage}"
            )
        }
    }*/

    /*override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Purchases updated.")
            purchases?.let {
                for (purchase in it) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (purchase.skus.contains(SkuConstants.CWAI_CREDIT_10) ||
                            purchase.skus.contains(SkuConstants.CWAI_CREDIT_20) ||
                            purchase.skus.contains(SkuConstants.CWAI_CREDIT_50) ||
                            purchase.skus.contains(SkuConstants.CWAI_CREDIT_100)
                        ) {
                            // Handle the consumable purchase of credit here
                            val consumeParams = ConsumeParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                            billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
                                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    Log.d(TAG, "Credit consumed successfully.")
                                    val sku = purchase.skus.getOrNull(0)
                                    if (sku != null) {
                                        successfulPurchaseCallback(sku,true)
                                    }
                                } else {
                                    Log.e(TAG, "Failed to consume credit: ${billingResult.debugMessage}")
                                    val sku = purchase.skus.getOrNull(0)
                                    if (sku != null) {
                                        successfulPurchaseCallback(sku,false)
                                    }
                                }
                            }
                        } else {
                            // Handle non-consumable purchase here
                            purchasesUpdatedListener?.invoke(it)
                        }
                    }
                }
            }
        } else {
            Log.e(
                TAG,
                "Purchases update failed: ${billingResult.debugMessage}"
            )
        }
    }*/

    @UiThread
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Purchases updated.")
            purchases?.let {
                for (purchase in it) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (purchase.skus.contains(SkuConstants.CWAI_CREDIT_10) ||
                            purchase.skus.contains(SkuConstants.CWAI_CREDIT_20) ||
                            purchase.skus.contains(SkuConstants.CWAI_CREDIT_50) ||
                            purchase.skus.contains(SkuConstants.CWAI_CREDIT_100)
                        ) {
                            // Handle the consumable purchase of credit here
                            val consumeParams = ConsumeParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                            billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
                                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    Log.d(TAG, "Credit consumed successfully.")
                                    val sku = purchase.skus.getOrNull(0)
                                    if (sku != null) {
                                        successfulPurchaseCallback(sku,true)
                                    }
                                } else {
                                    Log.e(TAG, "Failed to consume credit: ${billingResult.debugMessage}")
                                    val sku = purchase.skus.getOrNull(0)
                                    if (sku != null) {
                                        successfulPurchaseCallback(sku,false)
                                    }
                                }
                            }
                        } else if (purchase.skus.contains(SkuConstants.SubscriptionSku)) {
                            // Handle the subscription purchase of unli credit here
                            val sku = purchase.skus.getOrNull(0)
                            if (sku != null) {
                                successfulPurchaseCallback(sku,true)
                            }
                        } else {
                            // Handle non-consumable purchase here
                            purchasesUpdatedListener?.invoke(it)
                        }
                    }
                }
            }
        } else {
            Log.e(
                TAG,
                "Purchases update failed: ${billingResult.debugMessage}"
            )
        }
    }



    private fun responseCodeToMessage(responseCode: Int): String {
        return when (responseCode) {
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Service unavailable"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing unavailable"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Item unavailable"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Developer error"
            BillingClient.BillingResponseCode.ERROR -> "Error"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Item already owned"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "Item not owned"
            else -> "Unknown error"
        }
    }
}