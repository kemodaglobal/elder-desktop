package com.elderdesktop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "System boot completed. Initializing Elder Desktop process.")
            // By doing nothing more than receiving this broadcast, 
            // the app process is created and warmed up.
            // If any background services are needed in the future, start them here.
        }
    }
}
