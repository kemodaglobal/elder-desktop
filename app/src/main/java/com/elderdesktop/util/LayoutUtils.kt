package com.elderdesktop.util

import android.content.Context
import com.elderdesktop.R
import com.elderdesktop.model.AppInfo
import com.elderdesktop.model.AppType
import com.elderdesktop.model.DesktopConfig
import com.elderdesktop.model.DesktopXmlItem
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

object LayoutUtils {

    fun generateDefaultLayout(context: Context, allLaunchable: List<AppInfo>): List<String> {
        val initialOrder = mutableListOf<String>()
        initialOrder.add("widget:clock")

        val firstScreenMap = AppUtils.getFirstScreenPackageMap()
        val currentCountry = Locale.getDefault().country
        
        val xmlId = when (currentCountry) {
            "CN" -> R.xml.desktop_3x4_china
            "KP" -> R.xml.desktop_3x4_north_korea
            else -> R.xml.desktop_3x4_global
        }

        val config = parseDesktopConfig(context, xmlId)
        val used = mutableSetOf<String>()

        fun resolve(items: List<DesktopXmlItem>) {
            items.forEach { item ->
                when (item) {
                    is DesktopXmlItem.Type -> {
                        val pkg = firstScreenMap[item.type]?.firstOrNull { p -> allLaunchable.any { it.packageName == p } }
                        if (pkg != null && used.add(pkg)) initialOrder.add("app:$pkg")
                    }
                    is DesktopXmlItem.Package -> {
                        if (used.add(item.packageName)) initialOrder.add("app:${item.packageName}")
                    }
                }
            }
        }

        resolve(config.firstScreen)
        resolve(config.secondScreen)
        
        return initialOrder
    }

    fun parseDesktopConfig(context: Context, xmlId: Int): DesktopConfig {
        val firstScreen = mutableListOf<DesktopXmlItem>()
        val secondScreen = mutableListOf<DesktopXmlItem>()
        try {
            val parser = context.resources.getXml(xmlId)
            var eventType = parser.eventType
            var currentTag: String? = null
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "first-screen", "second-screen" -> currentTag = parser.name
                        "app" -> {
                            val typeStr = parser.getAttributeValue(null, "type")
                            val pkg = parser.getAttributeValue(null, "package")
                            val item = if (typeStr != null) {
                                try {
                                    DesktopXmlItem.Type(AppType.valueOf(typeStr))
                                } catch (_: Exception) {
                                    null
                                }
                            } else if (pkg != null) {
                                DesktopXmlItem.Package(pkg)
                            } else null
                            if (item != null) {
                                if (currentTag == "first-screen") firstScreen.add(item)
                                else if (currentTag == "second-screen") secondScreen.add(item)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        return DesktopConfig(firstScreen, secondScreen)
    }
}
