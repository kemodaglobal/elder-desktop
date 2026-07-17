package com.elderdesktop.notification

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.elderdesktop.R

@SuppressLint("StaticFieldLeak")
object FloatingNotificationManager {

    private var currentView: View? = null
    private var windowManager: WindowManager? = null

    @SuppressLint("InflateParams")
    fun showNotification(context: Context, sbn: StatusBarNotification) {
        if (currentView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(context)
        
        // We'll use a simple View-based overlay for now as it's easier to manage in a Service context
        // than a Compose-based overlay without a parent Activity.
        val view = inflater.inflate(R.layout.floating_notification, null)
        currentView = view

        val titleView = view.findViewById<TextView>(R.id.notif_title)
        val textView = view.findViewById<TextView>(R.id.notif_text)
        val btnClose = view.findViewById<Button>(R.id.btn_close)
        val btnOpen = view.findViewById<Button>(R.id.btn_open)

        val extras = sbn.notification.extras
        titleView.text = extras.getString("android.title") ?: "Notification"
        textView.text = extras.getCharSequence("android.text")?.toString() ?: ""

        btnClose.setOnClickListener {
            dismiss()
        }

        btnOpen.setOnClickListener {
            try {
                sbn.notification.contentIntent?.send()
            } catch (_: Exception) {}
            dismiss()
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
            width = (context.resources.displayMetrics.widthPixels * 0.8).toInt()
        }

        try {
            windowManager?.addView(view, params)
        } catch (e: Exception) {
            android.util.Log.e("FloatingNotif", "Error adding view", e)
        }
    }

    fun dismiss() {
        try {
            currentView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        currentView = null
    }
}
