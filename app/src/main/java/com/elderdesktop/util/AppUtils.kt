package com.elderdesktop.util

import android.content.Context
import android.content.Intent
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.AppType

object AppUtils {

    fun generateDefaultLayout(context: Context, allLaunchable: List<AppInfo>): List<String> {
        return LayoutUtils.generateDefaultLayout(context, allLaunchable)
    }

    fun launchApp(context: Context, app: AppInfo) {
        AppLauncher.launchApp(context, app)
    }

    fun launchCamera(context: Context) {
        AppLauncher.launchCamera(context)
    }

    fun launchDialer(context: Context) {
        AppLauncher.launchDialer(context)
    }

    fun launchContacts(context: Context) {
        AppLauncher.launchContacts(context)
    }

    fun launchSystemAssistant(context: Context) {
        AppLauncher.launchSystemAssistant(context)
    }

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

    fun getFirstScreenPackageMap(): Map<AppType, List<String>> = mapOf(
        AppType.WEATHER to listOf("com.elderdesktop"),
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
        ),
        AppType.CONTACTS to listOf(
            "com.android.contacts", "com.google.android.contacts",
            "com.samsung.android.app.contacts", "com.huawei.contacts"
        ),
        AppType.BROWSER to listOf(
            "com.android.chrome", "com.android.browser", "org.mozilla.firefox",
            "com.sec.android.app.sbrowser", "com.huawei.browser", "com.mias.browser"
        ),
        AppType.APP_STORE to listOf(
            "com.android.vending", "com.xiaomi.market", "com.huawei.appmarket",
            "com.oppo.market", "com.bbk.appstore", "com.sec.android.app.samsungapps"
        )
    )
}
