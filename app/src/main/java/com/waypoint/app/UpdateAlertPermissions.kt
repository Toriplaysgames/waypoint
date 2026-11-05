package com.waypoint.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/** One explanation, after onboarding, never over guidance or another permission flow. */
@Composable internal fun UpdateAlertPermissions(alerts:UpdateAlerts,ready:Boolean) {
    val context=LocalContext.current
    var pending by rememberSaveable { mutableStateOf(!alerts.explained) }
    var batteryStep by rememberSaveable { mutableStateOf(alerts.allowed()) }
    fun finish() { alerts.markExplained();pending=false }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted)batteryStep=true else finish()
    }
    if(!ready || !pending)return
    if(batteryStep && alerts.batteryUnrestricted()) {
        LaunchedEffect(Unit) { finish() }
        return
    }
    AlertDialog(onDismissRequest=::finish,
        title={Text(if(batteryStep)tr("Keep update alerts timely","התראות עדכון בזמן") else tr("Know when Waypoint is updated","לדעת כש-Waypoint מתעדכן"))},
        text={Text(if(batteryStep)
            tr("Waypoint checks for new releases about twice a day while the app is closed. Restricted battery access can delay these checks. In Android's battery settings, choose Waypoint and allow unrestricted background use if you want fewer delays. This may use more battery. Checks are automatic; downloads never are. You can skip this and still use the app.","Waypoint בודק אם יש גרסאות חדשות בערך פעמיים ביום כשהאפליקציה סגורה. הגבלות סוללה עלולות לעכב את הבדיקות. אם רוצים פחות עיכובים, בהגדרות הסוללה של Android בחרו ב-Waypoint ואפשרו שימוש ברקע ללא הגבלה. זה עשוי לצרוך יותר סוללה. הבדיקות אוטומטיות; ההורדות אף פעם לא. אפשר לדלג על זה ולהמשיך להשתמש באפליקציה.")
            else tr("Waypoint checks its GitHub releases automatically, without an account or paid service. Allow notifications to hear about new versions. Tapping an alert opens the update details; nothing is downloaded or installed until you choose. Alerts wait while you are driving.","Waypoint בודק את הגרסאות שלו ב-GitHub באופן אוטומטי, בלי חשבון ובלי שירות בתשלום. אפשרו התראות כדי לשמוע על גרסאות חדשות. הקשה על התראה פותחת את פרטי העדכון; שום דבר לא יורד או מותקן עד שבוחרים. בזמן נהיגה ההתראות ממתינות."))},
        confirmButton={TextButton(onClick={
            if(batteryStep) { finish();alerts.openBatterySettings() }
            else if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else { finish();alerts.openNotificationSettings() }
        }) { Text(if(batteryStep)tr("Review battery access","בדיקת הגישה לסוללה") else tr("Allow notifications","אישור התראות")) }},
        dismissButton={TextButton(onClick=::finish) { Text(tr("Not now","לא עכשיו")) }})
}

@Composable internal fun UpdateAlertStatus(alerts:UpdateAlerts) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val notifications=remember(lifecycle){alerts.allowed()}
    val unrestricted=remember(lifecycle){alerts.batteryUnrestricted()}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text(tr("Automatic update alerts","התראות עדכון אוטומטיות"),style=MaterialTheme.typography.titleMedium)
        Text(if(alerts.scheduled())tr("Enabled by default. Waypoint checks about twice a day when connected and the battery is not low. Android may delay checks. Downloads and installation are always your choice.","מופעל כברירת מחדל. Waypoint בודק בערך פעמיים ביום כשיש חיבור והסוללה לא חלשה. Android עשוי לעכב את הבדיקות. ההורדה וההתקנה תמיד בהחלטתך.")
            else tr("Background checks are unavailable on this device. Waypoint still checks when you open the app.","בדיקות ברקע אינן זמינות במכשיר הזה. Waypoint עדיין בודק כשפותחים את האפליקציה."),style=MaterialTheme.typography.bodyMedium)
        Text(if(notifications)tr("App update notifications are allowed.","התראות על עדכוני האפליקציה מאושרות.") else tr("Notifications are off. Allow App updates in Android to receive new-release alerts.","ההתראות כבויות. אפשרו את „עדכוני אפליקציה” ב-Android כדי לקבל התראות על גרסאות חדשות."),style=MaterialTheme.typography.bodyMedium)
        TextButton(onClick=alerts::openNotificationSettings,modifier=Modifier.heightIn(min=48.dp)) { Text(tr("Notification settings","הגדרות התראות")) }
        if(!unrestricted) {
            Text(tr("Battery restrictions can delay alerts while Waypoint is closed. In Android's battery settings, choose Waypoint to review background access. Allowing unrestricted use may consume more battery; it is not required to use Waypoint.","הגבלות סוללה עלולות לעכב התראות כש-Waypoint סגור. בהגדרות הסוללה של Android בחרו ב-Waypoint כדי לבדוק את הגישה ברקע. שימוש ללא הגבלה עשוי לצרוך יותר סוללה; הוא לא נדרש לשימוש ב-Waypoint."),style=MaterialTheme.typography.bodyMedium)
            TextButton(onClick=alerts::openBatterySettings,modifier=Modifier.heightIn(min=48.dp)) { Text(tr("Review battery access","בדיקת הגישה לסוללה")) }
        }
    }
}
