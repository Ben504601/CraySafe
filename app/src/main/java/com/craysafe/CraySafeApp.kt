package com.craysafe

import android.app.Application
import com.craysafe.api.ApiClient

class CraySafeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}