package com.elderdesktop.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo

object AppLauncher {

    fun launchApp(context: Context, app: AppInfo) {
        // Special handling for Xiaomi MIUI/HyperOS where Phone and Contacts share a package
        if (app.packageName == "com.android.contacts") {
            val phoneKeywords = context.getString(R.string.keyword_call).split(",")
            val contactKeywords = context.getString(R.string.keyword_contacts).split(",")

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
        val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(assistIntent)
            return
        } catch (_: Exception) { }

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

        val voiceCommandIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(voiceCommandIntent)
            return
        } catch (_: Exception) { }

        val googleSearchIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.VoiceSearchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(googleSearchIntent)
            return
        } catch (_: Exception) { }

        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(searchIntent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.unable_to_start_voice_assistant), Toast.LENGTH_SHORT).show()
        }
    }
}
