package com.jm4488.billingtest

import android.app.Application
import android.util.Log
import com.jm4488.billingtest.utils.GoogleBillingUtils

class GlobalApplication : Application() {
    val googleBillingUtils: GoogleBillingUtils
        get() = GoogleBillingUtils.getInstance(this)

    override fun onCreate() {
        super.onCreate()
        Log.e("[TEST]", "GlobalApplication onCreate")
        googleBillingUtils.initBillintClient()
    }
}