package com.jm4488.billingtest.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.billingclient.api.*
import com.jm4488.billingtest.GlobalApplication
import com.jm4488.billingtest.R
import com.jm4488.billingtest.adapter.BillingPurchaseAdapter
import com.jm4488.billingtest.data.Constants
import com.jm4488.billingtest.databinding.ActivityBillingPurchaseBinding
import com.jm4488.billingtest.utils.GoogleBillingUtils

class BillingPurchaseActivity : AppCompatActivity() {
    private lateinit var billingUtils: GoogleBillingUtils

    private lateinit var binding: ActivityBillingPurchaseBinding
    private lateinit var purchaseAdapter: BillingPurchaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing_purchase)
    }

    override fun onResume() {
        super.onResume()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_billing_purchase)
        billingUtils = (application as GlobalApplication).googleBillingUtils
//        lifecycle.addObserver(billingUtils)

        init()

        billingUtils.productSkuDetailsLiveData.observe(this, Observer {
            makeList(it)
        })

        binding.groupLoading.visibility = View.VISIBLE
        Handler().postDelayed({
            billingUtils.querySkuDetails(BillingClient.SkuType.INAPP, Constants.INAPP_PRODUCT_IDS)
        }, 500)
    }

    private fun init() {
        purchaseAdapter = BillingPurchaseAdapter(this)
        purchaseAdapter.setBillingUtils(billingUtils)
        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.itemAnimator?.let {
            when (it) {
                is SimpleItemAnimator -> it.supportsChangeAnimations = false
            }
        }
        binding.rvList.adapter = purchaseAdapter

        binding.btnLoadPurchases.setOnClickListener {
            billingUtils.querySkuDetails(BillingClient.SkuType.INAPP, Constants.INAPP_PRODUCT_IDS)
        }
    }

    private fun makeList(list: List<SkuDetails>) {
        Log.e("[INAPPACT]", "=== makeList ===")
        binding.groupLoading.visibility = View.GONE
        purchaseAdapter.items = ArrayList(list)
        purchaseAdapter.notifyDataSetChanged()
    }
}