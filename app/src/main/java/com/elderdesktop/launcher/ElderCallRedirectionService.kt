package com.elderdesktop.launcher

import android.net.Uri
import android.os.Build
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresApi
import com.elderdesktop.DesktopSettings

@RequiresApi(Build.VERSION_CODES.Q)
class ElderCallRedirectionService : CallRedirectionService() {

    override fun onPlaceCall(handle: Uri, selectedPhoneAccount: PhoneAccountHandle, confirmed: Boolean) {
        val settings = DesktopSettings(this)
        val phoneNumber = handle.schemeSpecificPart ?: ""

        if (phoneNumber.isEmpty()) {
            placeCallUnmodified()
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
            placeCallUnmodified()
            return
        }

        // 2. Intercept Logic
        var shouldBlock = false
        if (settings.interceptOverseasCalls && phoneNumber.startsWith("+") && !phoneNumber.startsWith("+86")) {
            shouldBlock = true
        } else if (settings.interceptSpamCalls && isCommonSpamPattern(phoneNumber)) {
            shouldBlock = true
        }

        if (shouldBlock) {
            cancelCall()
        } else {
            placeCallUnmodified()
        }
    }

    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "")
    }

    private fun isCommonSpamPattern(number: String): Boolean {
        val clean = normalizeNumber(number)
        return clean.startsWith("400") || clean.startsWith("95") || clean.length < 7
    }
}
