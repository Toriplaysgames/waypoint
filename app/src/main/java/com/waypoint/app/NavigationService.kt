package com.waypoint.app

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

class NavigationService : Service() {
    private val controller get() = (application as WaypointApplication).navigation
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var serviceSessionId: String?=null
    override fun onCreate() {
        super.onCreate()
        serviceSessionId=controller.navigationSessionId
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("navigation",tr("Active navigation","ניווט פעיל"),NotificationManager.IMPORTANCE_LOW))
        startForeground(10,notification(tr("Navigation is starting","הניווט מתחיל")))
        scope.launch {
            controller.state.map { s ->
                if(!s.navigating)null else if(!s.gpsReady)s.gpsStatus
                else if(s.routing)tr("Rerouting from your current position","מחשבים מסלול מחדש מהמיקום הנוכחי")
                else if(s.offRoute)tr("Off route · finding a new route","מחוץ למסלול · מחפשים מסלול חדש")
                else routeGuidance(s.route,s.progress).instruction
            }.distinctUntilChanged().collect { text ->
                if(text!=null)getSystemService(NotificationManager::class.java).notify(10,notification(text))
            }
        }
    }
    private fun notification(text: String): Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val token=controller.navigationSessionId
        val stopIntent=Intent(this,NavigationService::class.java).setAction("STOP")
            .setData(Uri.parse("waypoint://navigation/stop/${token ?: "inactive"}"))
            .putExtra("navigation_session",token)
        val stop=PendingIntent.getService(this,1,stopIntent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this,"navigation").setSmallIcon(R.drawable.ic_navigation)
            .setContentTitle("Waypoint · ${controller.state.value.selected?.name ?: tr("Navigation","ניווט")}")
            .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION).addAction(0,tr("End navigation","סיום הניווט"),stop).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action!="STOP")serviceSessionId=controller.navigationSessionId
        else {
            if(controller.acceptsNavigationSession(intent.getStringExtra("navigation_session"))) {
                controller.stopNavigation()
                stopSelf(startId)
            } else if(!controller.state.value.navigating) stopSelf(startId)
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); controller.serviceStopped(serviceSessionId); super.onDestroy() }
}
