package com.elderdesktop.util

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.elderdesktop.model.AppInfo

object TaskUtils {

    fun checkUsageStatsPermission(context: Context): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 60, currentTime)
        return stats != null && stats.isNotEmpty()
    }

    fun getRecentApps(context: Context): List<AppInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val currentTime = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            currentTime - 1000 * 60 * 60 * 24,
            currentTime
        ) ?: return emptyList()

        return stats.filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .sortedByDescending { it.lastTimeUsed }
            .mapNotNull { stat ->
                try {
                    val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                    AppInfo(
                        label = pm.getApplicationLabel(appInfo).toString(),
                        packageName = stat.packageName,
                        className = "", // We use launchIntent
                        icon = pm.getApplicationIcon(appInfo)
                    )
                } catch (_: Exception) {
                    null
                }
            }.distinctBy { it.packageName }
    }

    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
