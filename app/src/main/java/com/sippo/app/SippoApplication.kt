package com.sippo.app

import android.app.Application
import com.sippo.app.billing.BillingManager

class SippoApplication : Application() {
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        billing = BillingManager(this).also { it.connect() }
    }
}
