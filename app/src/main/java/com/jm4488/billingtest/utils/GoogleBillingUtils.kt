package com.jm4488.billingtest.utils

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.android.billingclient.api.*
import com.jm4488.billingtest.data.Constants
import org.json.JSONObject
import java.util.*

class GoogleBillingUtils  private constructor(
        private val app: Application
) : LifecycleObserver,
        PurchasesUpdatedListener,
        BillingClientStateListener,
        SkuDetailsResponseListener {

    private lateinit var billingClient: BillingClient
    val purchaseUpdateLiveData = MutableLiveData<List<Purchase>>()
    val alreadyPurchasedLiveData = MutableLiveData<ArrayList<Purchase>>()
    val productSkuDetailsLiveData = MutableLiveData<List<SkuDetails>>()

    val consumeCompleteLiveData = MutableLiveData<Purchase>()
    val acknowledgeCompleteLiveData = MutableLiveData<Purchase>()

    companion object {
        private const val TAG = "[GoogleBillingUtils]"

        @Volatile
        private var INSTANCE: GoogleBillingUtils? = null

        @JvmStatic
        fun getInstance(app: Application): GoogleBillingUtils =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: GoogleBillingUtils(app).also { INSTANCE = it }
                }
    }

    fun initBillintClient() {
        Log.e(TAG, "UTILS INIT")
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

    fun checkBillingClient(): Boolean {
        return if (billingClient.isReady) {
            Log.e(TAG, "BillingClient is ready")
            true
        } else {
            Log.e(TAG, "BillingClient is not ready")
            false
        }
    }

    /**
     * called after billingClient.startConnection()
     */
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        Log.e(TAG, "onBillingSetupFinished: $responseCode $debugMessage")
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // The billing client is ready. You can query purchases here.
        }
    }

    /**
     * called when billingClient is disconnected
     */
    override fun onBillingServiceDisconnected() {
        Log.e(TAG, "onBillingServiceDisconnected")
        if (!billingClient.isReady) {
            initBillintClient()
        }
    }

    /**
     * called after billingClient.querySkuDetailsAsync()
     * 구글 콘솔에 등록된 상품 목록이 호출된 후 결과이 전달되는 곳
     */
    override fun onSkuDetailsResponse(billingResult: BillingResult, mutableList: MutableList<SkuDetails>?) {
        Log.e(TAG, "=== onSkuDetailsResponse ===")
        Log.e(TAG, "result code : ${billingResult.responseCode}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            mutableList?.let {
                Log.e(TAG, "mutableList size : ${it.size}")
                productSkuDetailsLiveData.postValue(mutableList)
            } ?: productSkuDetailsLiveData.postValue(emptyList())
        } else {
            productSkuDetailsLiveData.postValue(emptyList())
        }
    }

    /**
     * called after billingClient.launchBillingFlow()
     * 인앱 구매 확인창 종료 후 호출됨
     * 여기서 결제를 대기 시키고 백엔드 서버에서 검증 후
     * 상품 종류에 따라 구매 확정 진행
     * 1. acknowledgePurchasePurchaseItem - 비소모성 상품일 경우 (구독 또는 일회용)
     * 2. consumePurchasePurchaseItem - 소모성 상품일 경우 (여러번 구매 가능한 상품)
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, mutableList: MutableList<Purchase>?) {
        Log.e(TAG, "=== onPurchasesUpdated ===")
        Log.e(TAG, "result code : ${billingResult.responseCode}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            mutableList?.let {
                Log.e(TAG, "mutableList size : ${it.size}")
                purchaseUpdateLiveData.postValue(mutableList)
//                handlePurchaseItems(mutableList)
            } ?: purchaseUpdateLiveData.postValue(emptyList())
        } else {
            purchaseUpdateLiveData.postValue(emptyList())
        }
    }

    fun queryAlreadyPurchases() {
        if (!checkBillingClient()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready")
        }
        Log.e(TAG, "queryPurchases: INAPP")
        val purchasedItems = arrayListOf<Purchase>()
        billingClient.queryPurchases(BillingClient.SkuType.INAPP)?.purchasesList?.let {
            for (item in it) {
                Log.e(TAG, "INAPP item purchaseState : ${item.purchaseState}")
            }
            purchasedItems.addAll(it)
        }

        Log.e(TAG, "queryPurchases: SUBS")
        billingClient.queryPurchases(BillingClient.SkuType.SUBS)?.purchasesList?.let {
            for (item in it) {
                Log.e(TAG, "SUBS item purchaseState : ${item.purchaseState}")
            }
            purchasedItems.addAll(it)
        }
        Log.e(TAG, "purchasedItems: ${purchasedItems.toString()}")
        alreadyPurchasedLiveData.postValue(purchasedItems)
    }

    fun querySkuDetails(type: String, productList: List<String>) {
        if (!checkBillingClient()) {
            Log.e(TAG, "querySkuDetails: BillingClient is not ready / $type / ${productList.toString()}")
        }
        Log.e(TAG, "querySkuDetails")
        val params = SkuDetailsParams.newBuilder()
                .setType(type)
                .setSkusList(productList)
                .build()
        params?.let { skuDetailsParams ->
            Log.e(TAG, "querySkuDetailsAsync")
            billingClient.querySkuDetailsAsync(skuDetailsParams, this)
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

    fun handlePurchaseItems(items: List<Purchase>) {
        if (!checkBillingClient()) {
            return
        }
        Log.e(TAG, "handlePurchaseItem items: ${items.size}")
        for (item in items) {
            Log.e(TAG, "item type : ${item.sku}")
            doConsumeOrAcknowledgePurchaseItem(item)
        }
    }

    /**
     * PENDING 체크
     * 문제점 있음
     * 1. 구매(launchBillingFlow) 진행 후 consume OR acknowledge 하지 않은 상태에서 상태 확인 시.
     *   - item.purchaseState = 1
     *   - item.originalJson.purchaseState = 0
     *   왜 다른가? 환불 관련이라는 글을 봤지만 아직 정확히 이해 못함...
     */
    fun isPurchasePending(item: Purchase): Boolean {
        val purchaseJsonObject = JSONObject(item.originalJson)
        val purchaseStateInJson = purchaseJsonObject.getInt("purchaseState")

        Log.e(TAG, "isPurchasePending purchaseState : ${item.purchaseState}")
        Log.e(TAG, "isPurchasePending isAcknowledged : ${item.isAcknowledged}")
        Log.e(TAG, "isPurchasePending isAutoRenewing : ${item.isAutoRenewing}")
        Log.e(TAG, "isPurchasePending originalJson : ${purchaseJsonObject.toString(4)}")
        Log.e(TAG, "isPurchasePending states / UNSPECIFIED_STATE: ${Purchase.PurchaseState.UNSPECIFIED_STATE} / PURCHASED: ${Purchase.PurchaseState.PURCHASED} / PENDING: ${Purchase.PurchaseState.PENDING}")
        Log.e(TAG, "isPurchasePending purchaseStateInJson : $purchaseStateInJson")
        Log.e(TAG, "isPurchasePending test : ${purchaseStateInJson == Purchase.PurchaseState.PURCHASED}")
        Log.e(TAG, "=====================================================")

        return if (item.isAcknowledged) {
            false
        } else purchaseStateInJson != Purchase.PurchaseState.PURCHASED
//
//        return if (!item.isAcknowledged && item.purchaseState == Purchase.PurchaseState.PENDING) {
//            true
//        } else if (item.isAcknowledged) {
//            false
//        } else if (item.purchaseState == Purchase.PurchaseState.PURCHASED) {
//            false
//        } else {
//            false
//        }
    }

    fun doConsumeOrAcknowledgePurchaseItem(item: Purchase) {
        when (item.sku) {
            in Constants.INAPP_PRODUCT_IDS -> {
                    consumePurchasePurchaseItem(item)
            }
            in Constants.SUBS_PRODUCT_IDS -> {
                    acknowledgePurchasePurchaseItem(item)
            }
            else -> {}
        }
    }

    // 비소비성 상품 청구 확인(구매확정) - 중복구매 불가
    fun acknowledgePurchasePurchaseItem(item: Purchase) {
        Log.e(TAG, "acknowledgePurchasePurchaseItem / ${item.purchaseToken}")
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(item.purchaseToken)
                .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            val responseCode = billingResult.responseCode
            val debugMessage = billingResult.debugMessage
            Log.e(TAG, "acknowledgePurchase: $responseCode $debugMessage")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                acknowledgeCompleteLiveData.postValue(item)
            }
        }
    }

    // 소비성 상품 청구 확인(구매확정) - 중복구매 가능
    fun consumePurchasePurchaseItem(item: Purchase) {
        Log.e(TAG, "consumePurchasePurchaseItem / ${item.purchaseToken}")
        val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(item.purchaseToken)
                        .build()
        billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
            val responseCode = billingResult.responseCode
            val debugMessage = billingResult.debugMessage
            Log.e(TAG, "consumePurchase: $responseCode $debugMessage / $outToken")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                consumeCompleteLiveData.postValue(item)
            }
        }
    }
}