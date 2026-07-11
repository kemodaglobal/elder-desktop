# ====== Launcher 核心入口必须保留 ======
# Home Activity 及其中 public 无参构造、onCreate 等生命周期不被混淆删除
-keep public class com.elderdesktop.DesktopActivity { *; }
-keep public class * extends android.app.Activity { *; }
-keep public class * extends android.app.Application { *; }
-keep public class * extends android.content.BroadcastReceiver { *; }
-keep public class * extends android.service.wallpaper.WallpaperService { *; }

# 保留注解（@Keep 生效）
-keepattributes *Annotation*
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }

# ====== 系统反射/PM查询相关 ======
# PackageManager.queryIntentActivities 靠 ComponentName 字符串，保留包名类名结构
-keep class com.elderdesktop.** { *; }

# 如果有用 Gson/Moshi 序列化设置(白名单配置等)，保留字段
# -keepclassmembers class com.example.seniorlauncher.data.** { <fields>; }

# ====== Full Mode 推荐补全 ======
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# 不警告缺失的 support 库引用
-dontwarn okio.**
-dontwarn kotlinx.**
-dontwarn android.view.HardwareCanvas
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
-dontwarn sun.misc.**
-dontwarn org.conscrypt.**
-dontwarn java.lang.instrument.**
