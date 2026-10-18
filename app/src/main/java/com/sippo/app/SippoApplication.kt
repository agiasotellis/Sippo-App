package com.sippo.app

import android.app.Application
import com.sippo.app.billing.BillingManager
import com.sippo.app.reminders.Reminders

class SippoApplication : Application() {
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
        billing = BillingManager(this).also { it.connect() }
    }
}
