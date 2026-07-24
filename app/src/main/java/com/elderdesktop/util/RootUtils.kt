package com.elderdesktop.util

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootUtils {

    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkSuBinary() || checkSuExistsCommand() || checkCommonRootPackages()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkSuExistsCommand(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val inReader = BufferedReader(InputStreamReader(process.inputStream))
            inReader.readLine() != null
        } catch (_: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    private fun checkCommonRootPackages(): Boolean {
        // Note: Full package check would require PackageManager,
        // but checking common file paths associated with these apps is a fallback.
        return false 
    }
}
