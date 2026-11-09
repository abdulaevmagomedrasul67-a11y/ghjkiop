package com.example.push

import android.app.Service
import android.content.Intent
import android.os.IBinder

class StudToolMessagingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "studtool_default"
    }
}
