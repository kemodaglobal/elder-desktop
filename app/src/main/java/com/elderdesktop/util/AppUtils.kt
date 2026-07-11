package com.elderdesktop.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.AppType

object AppUtils {

    fun getLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolvedInfos.map { info ->
            AppInfo(
                label = info.loadLabel(pm).toString(),
                packageName = info.activityInfo.packageName,
                className = info.activityInfo.name,
                icon = info.loadIcon(pm)
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun launchApp(context: Context, app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.className)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                context.startActivity(it)
            }
        }
    }

    fun launchCamera(context: Context) {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try { context.startActivity(intent) } catch (_: Exception) { }
    }

    fun getFirstScreenPackageMap(): Map<AppType, List<String>> = mapOf(
        AppType.GALLERY to listOf(
            "com.miui.gallery", "com.oplus.gallery", "com.google.android.apps.photos",
            "com.sec.android.gallery3d", "com.huawei.photos", "com.android.gallery3d"
        ),
        AppType.SETTINGS to listOf("com.android.settings"),
        AppType.DIALER to listOf(
            "com.android.contacts", "com.oplus.dialer", "com.google.android.dialer",
            "com.samsung.android.dialer", "com.android.phone"
        ),
        AppType.MESSAGING to listOf(
            "com.android.mms", "com.google.android.apps.messaging",
            "com.samsung.android.messaging", "com.huawei.message"
        ),
        AppType.CAMERA to listOf(
            "com.android.camera", "com.miui.camera", "com.google.android.GoogleCamera",
            "com.sec.android.app.camera", "com.huawei.camera"
        )
    )
}