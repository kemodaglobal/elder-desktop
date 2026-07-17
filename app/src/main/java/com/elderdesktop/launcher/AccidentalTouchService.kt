package com.elderdesktop.launcher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
        val settings = DesktopSettings(this)
        if (!settings.preventAccidentalTouch) return

        val rootNode = rootInActiveWindow ?: return

        if (settings.preventAdTouch) {
            scanForAds(rootNode)
        }

        if (settings.preventUnknownInstall) {
            interceptInstaller(event, rootNode)
        }
    }

    private fun scanForAds(node: AccessibilityNodeInfo) {
        val adKeywords = listOf("adview", "banner", "promotion", "广告", "广告栏", "推广")
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val resourceName = node.viewIdResourceName?.lowercase() ?: ""

        if (adKeywords.any { text.contains(it) || contentDesc.contains(it) || resourceName.contains(it) }) {
            // Attempt to block or warning
            // For seniors, we'll just log it for now or try to hide it if we had more advanced overlay logic
            android.util.Log.d("AccidentalTouchService", "Potential ad detected: $text / $resourceName")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanForAds(child)
            }
        }
    }

    private fun interceptInstaller(event: AccessibilityEvent?, rootNode: AccessibilityNodeInfo) {
        val packageName = event?.packageName?.toString() ?: ""
        if (packageName.contains("packageinstaller")) {
            val riskKeywords = listOf("unknown source", "risk", "unsafe", "未知来源", "风险", "不安全")
            if (findTextInNode(rootNode, riskKeywords)) {
                // Find "Cancel" or "Close" button
                val cancelKeywords = listOf("cancel", "deny", "refuse", "取消", "拒绝", "关闭")
                val cancelButton = findButtonWithText(rootNode, cancelKeywords)
                if (cancelButton != null) {
                    android.util.Log.w("AccidentalTouchService", "Intercepting risky installer. Clicking Cancel.")
                    cancelButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    // Fallback: Perform Global Back
                    android.util.Log.w("AccidentalTouchService", "Intercepting risky installer. Performing Back.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        }
    }

    private fun findTextInNode(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        if (keywords.any { text.contains(it) }) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && findTextInNode(child, keywords)) return true
        }
        return false
    }

    private fun findButtonWithText(node: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase() ?: ""
        if (node.isClickable && keywords.any { text.contains(it) }) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = child?.let { findButtonWithText(it, keywords) }
            if (result != null) return result
        }
        return null
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
