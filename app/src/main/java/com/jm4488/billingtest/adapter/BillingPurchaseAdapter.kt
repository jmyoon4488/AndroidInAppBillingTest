package com.jm4488.billingtest.adapter

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.jm4488.billingtest.databinding.ItemBillingProductBinding
import com.jm4488.billingtest.utils.GoogleBillingUtils
import kotlinx.android.synthetic.main.item_billing_product.view.*

class BillingPurchaseAdapter(activity: Activity) : RecyclerView.Adapter<BillingItemViewHolder>() {
    var items = arrayListOf<SkuDetails>()
    var activity = activity
    private lateinit var billingUtils: GoogleBillingUtils

    fun setBillingUtils(utils: GoogleBillingUtils) {
        billingUtils = utils
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillingItemViewHolder {
        return BillingItemViewHolder.ProductViewHolder(ItemBillingProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: BillingItemViewHolder, position: Int) {
        holder.onBind(items[position])
        holder.itemView.btn_buy.setOnClickListener {
            items[position].let {
                Log.e("[INAPPADAP]", "skuDetailsItem desc : ${it.toString()}")

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()

                when (billingUtils.launchBillingFlow(activity, billingFlowParams)) {
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : BILLING_UNAVAILABLE")
                    }
                    BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : DEVELOPER_ERROR")
                    }
                    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : FEATURE_NOT_SUPPORTED")
                    }
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : ITEM_ALREADY_OWNED")
                    }
                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : SERVICE_DISCONNECTED")
                    }
                    BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : SERVICE_TIMEOUT")
                    }
                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : ITEM_UNAVAILABLE")
                    }
                    else -> {
                        Log.e("[INAPPADAP]", "launchBillingFlow response : else")
                    }
                }
            }
        }
    }
}