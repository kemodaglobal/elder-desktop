package com.elderdesktop.launcher

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.elderdesktop.DesktopSettings
import com.elderdesktop.R
import com.elderdesktop.SettingsActivity
import com.elderdesktop.WeatherActivity
import com.elderdesktop.util.AppUtils
import com.elderdesktop.util.DeepSeekUtils
import com.elderdesktop.util.GoogleAiUtils
import com.elderdesktop.util.OpenAiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

object VoiceCommandHandler {

    fun handleVoiceCommand(
        command: String,
        context: Context,
        client: OkHttpClient,
        speak: (String) -> Unit,
        onTriggerUnlock: () -> Unit
    ) {
        val cameraKeywords = context.getString(R.string.keyword_camera).split(",")
        val settingsKeywords = context.getString(R.string.keyword_settings).split(",")
        val weatherKeywords = context.getString(R.string.keyword_weather).split(",")
        val callKeywords = context.getString(R.string.keyword_call).split(",")
        val contactKeywords = context.getString(R.string.keyword_contacts).split(",")
        val callPrefixes = context.getString(R.string.keyword_call_prefix).split(",")

        when {
            cameraKeywords.any { command.contains(it) } -> {
                speak(context.getString(R.string.opening_camera))
                AppUtils.launchCamera(context)
            }
            settingsKeywords.any { command.contains(it) } -> {
                val settings = DesktopSettings(context)
                if (settings.usePasscode) {
                    speak(context.getString(R.string.please_unlock_first))
                    onTriggerUnlock()
                } else {
                    speak(context.getString(R.string.opening_desktop_settings))
                    context.startActivity(Intent(context, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                }
            }
            weatherKeywords.any { command.contains(it) } -> {
                speak(context.getString(R.string.opening_weather))
                context.startActivity(Intent(context, WeatherActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
            contactKeywords.any { command.contains(it) && !callKeywords.any { c -> command.contains(c) } } -> {
                speak(context.getString(R.string.opening_phone))
                AppUtils.launchContacts(context)
            }
            callKeywords.any { command.contains(it) } -> {
                val settings = DesktopSettings(context)
                var targetName = ""

                for (pattern in callPrefixes) {
                    if (command.contains(pattern)) {
                        val after = command.substringAfter(pattern).trim()
                        if (after.isNotEmpty()) {
                            targetName = after
                            break
                        }
                        val before = command.substringBefore(pattern).trim()
                        if (before.isNotEmpty()) {
                            targetName = before
                            break
                        }
                    }
                }

                if (targetName.isNotEmpty()) {
                    var foundNumber: String? = null
                    for (i in 0 until 30) {
                        val contact = settings.getSpeedDial(i)
                        if (contact != null && contact.first.equals(targetName, ignoreCase = true)) {
                            foundNumber = contact.second
                            break
                        }
                    }

                    if (foundNumber != null) {
                        speak(context.getString(R.string.calling_name, targetName))
                        val intent = Intent(Intent.ACTION_CALL, "tel:$foundNumber".toUri()).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val dialIntent = Intent(Intent.ACTION_DIAL, "tel:$foundNumber".toUri()).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(dialIntent)
                        }
                    } else {
                        speak(context.getString(R.string.contact_not_found, targetName))
                    }
                } else {
                    speak(context.getString(R.string.opening_phone))
                    AppUtils.launchDialer(context)
                }
            }
            else -> {
                val settings = DesktopSettings(context)
                if (settings.enableDeepSeek) {
                    speak(context.getString(R.string.loading))
                    CoroutineScope(Dispatchers.IO).launch {
                        val reply = when (settings.aiProvider) {
                            "openai" -> OpenAiUtils.chat(prompt = command, apiKey = settings.openAiApiKey, client = client)
                            "google" -> GoogleAiUtils.chat(prompt = command, apiKey = settings.googleAiApiKey, client = client)
                            else -> DeepSeekUtils.chat(prompt = command, apiKey = settings.deepSeekApiKey, client = client)
                        }
                        withContext(Dispatchers.Main) {
                            if (reply != null) {
                                speak(reply)
                            } else {
                                speak(context.getString(R.string.command_not_recognized, command))
                            }
                        }
                    }
                } else {
                    speak(context.getString(R.string.command_not_recognized, command))
                }
            }
        }
    }
}
