package com.jm4488.billingtest

import android.app.Application
import android.util.Log

class GlobalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.e("[TEST]", "GlobalApplication onCreate")
    }
}