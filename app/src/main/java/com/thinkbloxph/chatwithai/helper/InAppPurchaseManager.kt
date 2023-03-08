package com.thinkbloxph.chatwithai.helper

import android.app.Activity
import android.util.Log
import androidx.annotation.UiThread
import com.android.billingclient.api.*

class InAppPurchaseManager(private val activity: Activity) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private lateinit var skuDetailsMap: Map<String, SkuDetails>
    private var purchasesUpdatedListener: ((List<Purchase>) -> Unit)? = null

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
        val skuList = listOf("cwai_credit_10", "cwai_credit_50","cwai_credit_100")
        val params = SkuDetailsParams.newBuilder()
            .setType(BillingClient.SkuType.INAPP)
            .setSkusList(skuList)
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Sku details query succeeded.")
                skuDetailsMap = skuDetailsList!!.associateBy { it.sku }
            } else {
                Log.e(TAG, "Sku details query failed: ${billingResult.debugMessage}")
            }
        }
    }

    /*private fun querySkuSubscriptionDetails(){
        val skuList = listOf("premium_upgrade", "monthly_subscription", "yearly_subscription")
        val paramsBuilder = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)

        paramsBuilder.setType(BillingClient.SkuType.SUBS)
        val subsParams = paramsBuilder.build()
        billingClient.querySkuDetailsAsync(subsParams) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Sku details query succeeded.")
                skuDetailsMap = skuDetailsList!!.associateBy { it.sku }
            } else {
                Log.e(TAG, "Sku details query failed: ${billingResult.debugMessage}")
            }
    }*/

    private fun queryPurchases() {
        val purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        if (purchasesResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Purchases query succeeded.")
            purchasesResult.purchasesList?.let { purchasesUpdatedListener?.invoke(it) }
        } else {
            //Log.e(TAG, "Purchases query failed: ${purchasesResult.debugMessage}")
        }
    }

    fun purchase(sku: String) {
        val skuDetails = skuDetailsMap[sku] ?: run {
            Log.e(TAG, "Sku details not found for $sku.")
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

    fun setOnPurchasesUpdatedListener(listener: (List<Purchase>) -> Unit) {
        purchasesUpdatedListener = listener
    }

    @UiThread
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

    companion object {
        private const val TAG = "InAppPurchaseManager"
    }
}