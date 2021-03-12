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
import com.jm4488.billingtest.adapter.BillingSubscribeAdapter
import com.jm4488.billingtest.data.Constants
import com.jm4488.billingtest.databinding.ActivityBillingSubscribeBinding
import com.jm4488.billingtest.utils.GoogleBillingUtils

class BillingSubscribeActivity : AppCompatActivity() {
    private lateinit var billingUtils: GoogleBillingUtils

    private lateinit var binding: ActivityBillingSubscribeBinding
    private lateinit var subscribeAdapter: BillingSubscribeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing_subscribe)
    }

    override fun onResume() {
        super.onResume()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_billing_subscribe)
        billingUtils = (application as GlobalApplication).googleBillingUtils
//        lifecycle.addObserver(billingUtils)

        init()

        billingUtils.productSkuDetailsLiveData.observe(this, Observer {
            makeList(it)
        })

        binding.groupLoading.visibility = View.VISIBLE
        Handler().postDelayed({
            billingUtils.querySkuDetails(BillingClient.SkuType.SUBS, Constants.SUBS_PRODUCT_IDS)
        }, 500)
    }

    private fun init() {
        subscribeAdapter = BillingSubscribeAdapter(this)
        subscribeAdapter.setBillingUtils(billingUtils)
        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.itemAnimator?.let {
            when (it) {
                is SimpleItemAnimator -> it.supportsChangeAnimations = false
            }
        }
        binding.rvList.adapter = subscribeAdapter

        binding.btnLoadSubscriptions.setOnClickListener {
            billingUtils.querySkuDetails(BillingClient.SkuType.SUBS, Constants.SUBS_PRODUCT_IDS)
        }
    }

    private fun makeList(list: List<SkuDetails>) {
        Log.e("[SUBSACT]", "=== makeList ===")
        binding.groupLoading.visibility = View.GONE
        subscribeAdapter.items = ArrayList(list)
        subscribeAdapter.notifyDataSetChanged()
    }
}