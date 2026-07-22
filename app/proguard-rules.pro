-keep public class com.elderdesktop.launcher.Launcher { *; }
-keep public class * extends android.app.Activity { *; }
-keep public class * extends android.app.Application { *; }
-keep public class * extends android.content.BroadcastReceiver { *; }
-keep public class * extends android.service.wallpaper.WallpaperService { *; }

-keepattributes *Annotation*
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }

-keep class com.elderdesktop.** { *; }

-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

-dontwarn okio.**
-dontwarn kotlinx.**
-dontwarn android.view.HardwareCanvas
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
-dontwarn sun.misc.**
-dontwarn org.conscrypt.**
-dontwarn java.lang.instrument.**
