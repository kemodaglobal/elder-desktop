package com.elderdesktop.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.core.text.HtmlCompat
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.elderdesktop.BuildConfig
import com.elderdesktop.R
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AboutPage() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val version = BuildConfig.VERSION_NAME
    val buildType = if (BuildConfig.DEBUG) "debug" else "release"
    val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm", LocalLocale.current.platformLocale)
        .format(Date(BuildConfig.BUILD_TIME))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(text = stringResource(R.string.version_label, version, buildType))
        Text(text = stringResource(R.string.build_time_label, buildTime))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW,
                    "https://github.com/kemodaglobal/elder-desktop".toUri())
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.view_github))
        }
        
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW,
                    "https://gitlab.com/kemodaglobal/elder-desktop".toUri())
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.view_gitlab))
        }

        Spacer(modifier = Modifier.height(8.dp))

        var showPrivacyDialog by remember { mutableStateOf(false) }
        
        if (showPrivacyDialog) {
            Dialog(onDismissRequest = { showPrivacyDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.privacy_policy),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Text(
                                text = HtmlCompat.fromHtml(stringResource(R.string.privacy_policy_content), HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showPrivacyDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }
            }
        }

        TextButton(onClick = { showPrivacyDialog = true }) {
            Text(stringResource(R.string.privacy_policy))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.designed_with_love),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.open_source_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
