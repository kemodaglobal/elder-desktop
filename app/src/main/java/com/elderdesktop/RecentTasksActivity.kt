package com.elderdesktop

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.elderdesktop.ui.theme.ElderDesktopTheme
import com.elderdesktop.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecentTasksActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()
        setContent {
            ElderDesktopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.8f) // Dark minimalist background
                ) {
                    RecentTasksScreen()
                }
            }
        }
    }
}

@Composable
fun RecentTasksScreen() {
    val context = LocalContext.current
    var recentApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(checkUsageStatsPermission(context)) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            recentApps = withContext(Dispatchers.IO) {
                getRecentApps(context)
            }
        }
    }

    if (!hasPermission) {
        PermissionRequestView {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.recent_tasks),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (recentApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_recent_tasks), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recentApps) { app ->
                        RecentTaskItem(app) {
                            launchApp(context, app.packageName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTaskItem(app: AppInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun PermissionRequestView(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.usage_access_message),
            color = Color.LightGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.open_settings))
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun getRecentApps(context: Context): List<AppInfo> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager
    val time = System.currentTimeMillis()
    
    // Get usage stats for the last 24 hours
    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        time - 1000 * 60 * 60 * 24,
        time
    )

    if (stats == null) return emptyList()

    // Filter and sort by last time used, excluding our own app
    return stats
        .asSequence()
        .filter { it.lastTimeUsed > 0 && it.packageName != context.packageName }
        .sortedByDescending { it.lastTimeUsed }
        .mapNotNull { stat ->
            try {
                val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                AppInfo(
                    label = pm.getApplicationLabel(appInfo).toString(),
                    packageName = stat.packageName,
                    className = "", // We'll use launch intent for simplicity
                    icon = pm.getApplicationIcon(appInfo)
                )
            } catch (_: Exception) {
                null
            }
        }
        .distinctBy { it.packageName }
        .take(10)
        .toList() // Show top 10 recent apps
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
