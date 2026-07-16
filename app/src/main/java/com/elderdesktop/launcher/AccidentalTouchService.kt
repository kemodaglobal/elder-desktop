package com.elderdesktop.launcher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.elderdesktop.DesktopSettings

@SuppressLint("AccessibilityPolicy")
class AccidentalTouchService : AccessibilityService() {

    private var topOverlay: View? = null
    private var bottomOverlay: View? = null
    private lateinit var windowManager: WindowManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        updateOverlays()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for overlay logic
    }

    override fun onInterrupt() {
        removeOverlays()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        removeOverlays()
        return super.onUnbind(intent)
    }

    private fun updateOverlays() {
        val settings = DesktopSettings(this)
        if (settings.preventAccidentalTouch) {
            addOverlays()
        } else {
            removeOverlays()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addOverlays() {
        if (topOverlay != null || bottomOverlay != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            80, // 80px height for protection zone
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        topOverlay = View(this).apply {
            setBackgroundColor(0x01000000) // Nearly transparent
            setOnTouchListener { _, _ -> true } // Consume touch
        }

        bottomOverlay = View(this).apply {
            setBackgroundColor(0x01000000)
            setOnTouchListener { _, _ -> true }
        }

        val topParams = WindowManager.LayoutParams().apply {
            copyFrom(params)
            gravity = Gravity.TOP
        }

        val bottomParams = WindowManager.LayoutParams().apply {
            copyFrom(params)
            gravity = Gravity.BOTTOM
        }

        try {
            windowManager.addView(topOverlay, topParams)
            windowManager.addView(bottomOverlay, bottomParams)
        } catch (e: Exception) {
            android.util.Log.e("AccidentalTouchService", "Error adding overlays", e)
        }
    }

    private fun removeOverlays() {
        try {
            topOverlay?.let { windowManager.removeView(it) }
            bottomOverlay?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            android.util.Log.e("AccidentalTouchService", "Error removing overlays", e)
        } finally {
            topOverlay = null
            bottomOverlay = null
        }
    }
    
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        updateOverlays()
        return super.onStartCommand(intent, flags, startId)
    }
}
