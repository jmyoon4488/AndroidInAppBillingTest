package com.jm4488.billingtest.utils

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.android.billingclient.api.*
import com.jm4488.billingtest.data.Constants

class GoogleBillingUtilsDev private constructor(
        private val app: Application
) : PurchasesUpdatedListener,
        BillingClientStateListener,
        SkuDetailsResponseListener {

    private lateinit var billingClient: BillingClient
    val purchaseUpdateEvent = SingleLiveEvent<List<Purchase>>()
    val purchases = MutableLiveData<List<Purchase>>()
    val skusWithSkuDetails = MutableLiveData<Map<String, SkuDetails>>()
    var isBillingClientConnected = MutableLiveData<Boolean>()

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.e(TAG, "onBillingSetupFinished: $responseCode $debugMessage")
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // The billing client is ready. You can query purchases here.
                isBillingClientConnected.postValue(true)
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.e(TAG, "onBillingServiceDisconnected")
        isBillingClientConnected.postValue(false)
        // TODO: Try connecting again with exponential backoff.
        // billingClient.startConnection(this)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.e(TAG, "onPurchasesUpdated: $responseCode $debugMessage")
        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases == null) {
                    Log.e(TAG, "onPurchasesUpdated: null purchase list")
                    processPurchases(null)
                } else {
                    processPurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.e(TAG, "onPurchasesUpdated: User canceled the purchase")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.e(TAG, "onPurchasesUpdated: The user already owns this item")
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                Log.e(TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
                )
            }
        }
    }

    override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: MutableList<SkuDetails>?) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.e(TAG, "onSkuDetailsResponse: $responseCode $debugMessage")
                if (skuDetailsList == null) {
                    Log.e(TAG, "onSkuDetailsResponse: null SkuDetails list")
                    skusWithSkuDetails.postValue(emptyMap())
                } else
                    skusWithSkuDetails.postValue(HashMap<String, SkuDetails>().apply {
                        for (details in skuDetailsList) {
                            put(details.sku, details)
                        }
                    }.also { postedValue ->
                        Log.e(TAG, "onSkuDetailsResponse: count ${postedValue.size}")
                    })
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ERROR -> {
                Log.e(TAG, "onSkuDetailsResponse: $responseCode $debugMessage")
            }
            BillingClient.BillingResponseCode.USER_CANCELED,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                // These response codes are not expected.
                Log.e(TAG, "onSkuDetailsResponse: $responseCode $debugMessage")
            }
        }
    }

    fun querySkuDetails() {
        Log.e(TAG, "querySkuDetails")
        val params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.SUBS)
                .setSkusList(listOf(
                        Constants.BASIC_SKU,
                        Constants.PREMIUM_SKU
                ))
                .build()
        params?.let { skuDetailsParams ->
            Log.e(TAG, "querySkuDetailsAsync")
            billingClient.querySkuDetailsAsync(skuDetailsParams, this)
        }
    }

    fun queryPurchases() {
        if (!checkBillingClient()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready")
        }
        Log.e(TAG, "queryPurchases: SUBS")
        val result = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        if (result == null) {
            Log.e(TAG, "queryPurchases: null purchase result")
            processPurchases(null)
        } else {
            if (result.purchasesList == null) {
                Log.e(TAG, "queryPurchases: null purchase list")
                processPurchases(null)
            } else {
                processPurchases(result.purchasesList)
            }
        }
    }

    private fun processPurchases(purchasesList: List<Purchase>?) {
        Log.e(TAG, "processPurchases: ${purchasesList?.size} purchase(s)")
        if (isUnchangedPurchaseList(purchasesList)) {
            Log.e(TAG, "processPurchases: Purchase list has not changed")
            return
        }
        purchaseUpdateEvent.postValue(purchasesList)
        purchases.postValue(purchasesList)
        purchasesList?.let {
            logAcknowledgementStatus(purchasesList)
        }
    }

    fun launchBillingFlow(activity: Activity, params: BillingFlowParams): Int {
        val sku = params.sku
        val oldSku = params.oldSku
        Log.e(TAG, "launchBillingFlow: sku: $sku, oldSku: $oldSku")
        if (!checkBillingClient()) {
            Log.e(TAG, "launchBillingFlow: BillingClient is not ready")
        }
        val billingResult = billingClient.launchBillingFlow(activity, params)
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.e(TAG, "launchBillingFlow: BillingResponse $responseCode $debugMessage")
        return responseCode
    }

    private fun isUnchangedPurchaseList(purchasesList: List<Purchase>?): Boolean {
        // TODO: Optimize to avoid updates with identical data.
        return false
    }

    private fun logAcknowledgementStatus(purchasesList: List<Purchase>) {
        var ack_yes = 0
        var ack_no = 0
        for (purchase in purchasesList) {
            if (purchase.isAcknowledged) {
                ack_yes++
            } else {
                ack_no++
            }
        }
        Log.e(TAG, "logAcknowledgementStatus: acknowledged=$ack_yes unacknowledged=$ack_no")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        Log.e(TAG, "ON_CREATE")
        // Create a new BillingClient in onCreate().
        // Since the BillingClient can only be used once, we need to create a new instance
        // after ending the previous connection to the Google Play Store in onDestroy().
        billingClient = BillingClient.newBuilder(app.applicationContext)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build()
        if (!checkBillingClient()) {
            Log.e(TAG, "BillingClient: Start connection...")
            billingClient.startConnection(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        Log.e(TAG, "ON_DESTROY")
        if (checkBillingClient()) {
            Log.e(TAG, "BillingClient can only be used once -- closing connection")
            // BillingClient can only be used once.
            // After calling endConnection(), we must create a new BillingClient.
            billingClient.endConnection()
        }
    }

    fun checkBillingClient(): Boolean {
        return if (billingClient.isReady) {
            Log.e(TAG, "BillingClient is ready")
            true
        } else {
            Log.e(TAG, "BillingClient is not ready")
            false
        }
    }

    companion object {
        private const val TAG = "[GoogleBillingUtilsDev]"

        @Volatile
        private var INSTANCE: GoogleBillingUtilsDev? = null

        @JvmStatic
        fun getInstance(app: Application): GoogleBillingUtilsDev =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: GoogleBillingUtilsDev(app).also { INSTANCE = it }
                }
    }
}