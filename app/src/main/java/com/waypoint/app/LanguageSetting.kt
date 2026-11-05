package com.waypoint.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Lets people use Waypoint in Hebrew or English regardless of the phone language. */
@Composable
internal fun LanguageSetting(muted: Color) {
    val context = LocalContext.current
    var choice by remember { mutableStateOf(AppLanguage.current(context)) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Language", "שפה"), style = MaterialTheme.typography.titleMedium)
        if (!AppLanguage.canChooseInApp) {
            Text(tr("Waypoint follows your phone language. To use Hebrew, choose Hebrew in Android language settings.",
                "Waypoint משתמש בשפת הטלפון. כדי להשתמש באנגלית, בחרו אנגלית בהגדרות השפה של Android."),
                style = MaterialTheme.typography.bodySmall, color = muted)
        } else Column(Modifier.selectableGroup()) {
            for (option in AppLanguage.Choice.entries) {
                val label = when (option) {
                    AppLanguage.Choice.SYSTEM -> tr("Phone language", "שפת הטלפון")
                    AppLanguage.Choice.ENGLISH -> "English"
                    AppLanguage.Choice.HEBREW -> "עברית"
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = choice == option, role = Role.RadioButton, onClick = {
                    if (choice != option && AppLanguage.choose(context, option)) choice = option
                }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = choice == option, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Text(tr("Hebrew includes right-to-left screens, Hebrew map labels where available, and Hebrew spoken directions when a Hebrew text-to-speech voice is installed.",
            "העברית כוללת מסכים מימין לשמאל, שמות במפה בעברית היכן שיש, והנחיות קוליות בעברית כשמותקן קול עברי להקראת טקסט."),
            style = MaterialTheme.typography.bodySmall, color = muted)
    }
}
