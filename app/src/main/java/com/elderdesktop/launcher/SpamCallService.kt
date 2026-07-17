package com.elderdesktop.launcher

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.elderdesktop.DesktopSettings

@RequiresApi(Build.VERSION_CODES.N)
class SpamCallService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val settings = DesktopSettings(this)
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        
        if (phoneNumber.isEmpty()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // 1. Check Whitelist (Speed Dial contacts)
        var isWhitelisted = false
        for (i in 0 until 30) {
            val contact = settings.getSpeedDial(i)
            if (contact != null && normalizeNumber(phoneNumber) == normalizeNumber(contact.second)) {
                isWhitelisted = true
                break
            }
        }

        if (isWhitelisted) {
            Log.d("SpamCallService", "Allowing whitelisted contact: $phoneNumber")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // 2. Intercept Logic
        var shouldBlock = false
        var reason = ""

        if (settings.interceptOverseasCalls && phoneNumber.startsWith("+") && !phoneNumber.startsWith("+86")) {
            // Heuristic for China (+86). In a real app, this should be dynamic based on locale.
            shouldBlock = true
            reason = "Overseas Call"
        } else if (settings.interceptSpamCalls && isCommonSpamPattern(phoneNumber)) {
            shouldBlock = true
            reason = "Spam Pattern"
        }

        if (shouldBlock) {
            Log.w("SpamCallService", "Blocking call: $phoneNumber Reason: $reason")
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            respondToCall(callDetails, response)
        } else {
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }

    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "")
    }

    private fun isCommonSpamPattern(number: String): Boolean {
        // Very basic spam detection
        val clean = normalizeNumber(number)
        return clean.startsWith("400") || clean.startsWith("95") || clean.length < 7
    }
}
