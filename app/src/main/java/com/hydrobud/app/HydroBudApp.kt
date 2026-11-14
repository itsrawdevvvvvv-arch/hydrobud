package com.hydrobud.app

import android.app.Application
import com.hydrobud.app.notifications.HydrationNotificationHelper

class HydroBudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HydrationNotificationHelper.createNotificationChannel(this)
    }
}
