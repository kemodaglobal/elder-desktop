package com.elderdesktop.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
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
        // Special handling for Xiaomi MIUI/HyperOS where Phone and Contacts share a package
        if (app.packageName == "com.android.contacts") {
            val phoneKeywords = context.getString(com.elderdesktop.R.string.keyword_call).split(",")
            val contactKeywords = context.getString(com.elderdesktop.R.string.keyword_contacts).split(",")

            val specialClassName = when {
                phoneKeywords.any { app.label.contains(it, ignoreCase = true) } -> 
                    "com.android.contacts.activities.TwelveKeyDialer"
                contactKeywords.any { app.label.contains(it, ignoreCase = true) } ->
                    "com.android.contacts.activities.PeopleActivity"
                else -> null
            }
            
            if (specialClassName != null) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(app.packageName, specialClassName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                    // Fallback to normal launch if special activity fails
                }
            }
        }

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

    fun launchDialer(context: Context) {
        // Preference for Xiaomi MIUI/HyperOS specific dialer
        val miuiIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.android.contacts", "com.android.contacts.activities.TwelveKeyDialer")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(miuiIntent)
        } catch (_: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(dialIntent) } catch (_: Exception) { }
        }
    }

    fun launchContacts(context: Context) {
        // Preference for Xiaomi MIUI/HyperOS specific contacts
        val miuiIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.android.contacts", "com.android.contacts.activities.PeopleActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(miuiIntent)
        } catch (_: Exception) {
            val contactsIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "vnd.android.cursor.dir/contact"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(contactsIntent) } catch (_: Exception) { }
        }
    }

    fun launchSystemAssistant(context: Context) {
        // 1. Try the official Android "Assist" shortcut (Most universal)
        // This will launch whatever the user has set as their "Default Assistant"
        // (e.g. Google Assistant, Bixby, Xiao Ai, etc.)
        val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(assistIntent)
            return
        } catch (_: Exception) { }

        // 2. Try Xiaomi Hyper AI (Xiao Ai) specific service
        val xiaoAiIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.miui.voiceassist", "com.miui.voiceassist.VoiceService")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(xiaoAiIntent)
            return
        } catch (_: Exception) {
            val xiaoAiGeneral = context.packageManager.getLaunchIntentForPackage("com.miui.voiceassist")
            if (xiaoAiGeneral != null) {
                context.startActivity(xiaoAiGeneral)
                return
            }
        }

        // 3. Try Standard Voice Command
        val voiceCommandIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(voiceCommandIntent)
            return
        } catch (_: Exception) { }

        // 4. Try Google App Search
        val googleSearchIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.VoiceSearchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(googleSearchIntent)
            return
        } catch (_: Exception) { }

        // 5. Fallback to Web Search
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(searchIntent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(com.elderdesktop.R.string.unable_to_start_voice_assistant), Toast.LENGTH_SHORT).show()
        }
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