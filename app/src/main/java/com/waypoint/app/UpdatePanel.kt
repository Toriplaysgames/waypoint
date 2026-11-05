package com.waypoint.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun UpdatePanel(updater: AppUpdater, navigating: Boolean) {
    val status by updater.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    var installMessage by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("App updates","עדכוני אפליקציה"), style = MaterialTheme.typography.titleLarge)
        Text("Waypoint ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = Muted)
        when (val current = status) {
            UpdateStatus.Idle -> Text(tr("Waypoint checks GitHub for stable releases when you open the app. Downloads begin only when you ask.","Waypoint בודק ב-GitHub אם יש גרסה יציבה חדשה כשפותחים את האפליקציה. ההורדה מתחילה רק כשמבקשים."), style = MaterialTheme.typography.bodyMedium)
            UpdateStatus.Checking -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(tr("Checking the latest release…","בודקים את הגרסה האחרונה…"), style = MaterialTheme.typography.bodyMedium)
            }
            UpdateStatus.Current -> Text(tr("You're up to date.","הגרסה שלך עדכנית."), style = MaterialTheme.typography.bodyMedium)
            is UpdateStatus.Available -> {
                Text(tr("Version ${current.release.version} is available","גרסה ${current.release.version} זמינה"), style = MaterialTheme.typography.titleMedium)
                Text(tr("${String.format(Locale.getDefault(), "%.1f", current.release.size / 1048576.0)} MB · Android will ask before installing.","${String.format(Locale.getDefault(), "%.1f", current.release.size / 1048576.0)} MB · Android יבקש אישור לפני ההתקנה."), style = MaterialTheme.typography.bodyMedium, color = Muted)
                if (current.release.notes.isNotBlank()) ReleaseNotesContent(current.release.notes)
                Button(onClick = { updater.download(current.release) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Download, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Download update","הורדת העדכון"))
                }
            }
            is UpdateStatus.Downloading -> {
                LinearProgressIndicator(progress = { current.percent / 100f }, modifier = Modifier.fillMaxWidth())
                Text(if (current.percent == 100) tr("Verifying update…","מאמתים את העדכון…") else tr("Downloading update · ${current.percent}%","מורידים את העדכון · ${current.percent}%"), style = MaterialTheme.typography.bodyMedium)
                if (current.release.notes.isNotBlank()) ReleaseNotesContent(current.release.notes)
                TextButton(onClick = updater::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("Cancel download","ביטול ההורדה")) }
            }
            is UpdateStatus.Ready -> {
                Text(tr("Version ${current.release.version} is ready","גרסה ${current.release.version} מוכנה"), style = MaterialTheme.typography.titleMedium)
                Text(if (navigating) tr("End navigation before installing. Your saved places will be kept.","סיימו את הניווט לפני ההתקנה. המקומות השמורים יישמרו.") else tr("Download verified. Android may ask you to allow updates from Waypoint. Your saved places will be kept.","ההורדה אומתה. ייתכן ש-Android יבקש לאשר עדכונים מ-Waypoint. המקומות השמורים יישמרו."), style = MaterialTheme.typography.bodyMedium)
                if (current.release.notes.isNotBlank()) ReleaseNotesContent(current.release.notes)
                Button(onClick = { installMessage = updater.install(context, current) }, enabled = !navigating, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.SystemUpdate, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Install update","התקנת העדכון"))
                }
                installMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            }
            is UpdateStatus.Failed -> Text(current.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (status == UpdateStatus.Idle || status == UpdateStatus.Current || status is UpdateStatus.Failed) {
            OutlinedButton(onClick = updater::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (status is UpdateStatus.Failed) tr("Try again","ניסיון חוזר") else tr("Check for updates","בדיקת עדכונים"))
            }
        }
        UpdateAlertStatus((context.applicationContext as WaypointApplication).updateAlerts)
        TextButton(onClick = { uri.openUri(ReleaseUpdate.PAGE) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("Release notes on GitHub","הערות הגרסה ב-GitHub")) }
    }
}
