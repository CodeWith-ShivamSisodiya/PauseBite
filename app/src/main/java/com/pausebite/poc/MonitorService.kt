package com.pausebite.poc

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class MonitorService : Service() {
 private val handler=Handler(Looper.getMainLooper())
 private val targets=mapOf("com.application.zomato" to "Zomato","in.swiggy.android" to "Swiggy")
 private var lastForeground=""
 private var cursor=System.currentTimeMillis()
 private val tick=object:Runnable {
  override fun run() {
   try { poll() } catch(e:Exception) { Log.e("PauseBite","Monitor failed",e) }
   handler.postDelayed(this,900)
  }
 }
 override fun onCreate() { super.onCreate(); startNotification(); handler.post(tick) }
 private fun startNotification() {
  val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel("monitor","PauseBite monitoring",NotificationManager.IMPORTANCE_LOW))
  val stop=PendingIntent.getService(this,1,Intent(this,MonitorService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notification=Notification.Builder(this,"monitor").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("PauseBite monitoring is active").setContentText("Watching selected food apps; tap Stop to end").addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
  if(Build.VERSION.SDK_INT>=34) startForeground(42,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(42,notification)
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(intent?.action=="STOP") { stopSelf(); return START_NOT_STICKY }
  return START_NOT_STICKY
 }
 private fun poll() {
  val now=System.currentTimeMillis()
  val events=(getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager).queryEvents(cursor,now)
  val event=UsageEvents.Event()
  var newest=cursor
  var foreground:String?=null
  while(events.hasNextEvent()) {
   events.getNextEvent(event)
   if(event.eventType==UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp>=newest) {
    newest=event.timeStamp
    foreground=event.packageName
   }
  }
  cursor=now
  if(foreground!=null && foreground!=lastForeground) {
   lastForeground=foreground
   val name=targets[foreground]
   if(name!=null) {
    val p=getSharedPreferences("poc",MODE_PRIVATE)
    val delay=now-newest
    p.edit().putString("last_detected",name).putLong("delay_ms",delay).apply()
    if(now>=p.getLong("snooze_until",0) && now>=p.getLong("last_popup_at",0)+30000L) {
     Overlay.show(this,name)
     p.edit().putLong("last_popup_at",now).putInt("popup_count",p.getInt("popup_count",0)+1).apply()
     Log.i("PauseBite","Detected $name after ${delay}ms")
    }
   } else Overlay.hide()
  }
 }
 override fun onDestroy() { handler.removeCallbacks(tick); Overlay.hide(); super.onDestroy() }
 override fun onBind(intent:Intent?):IBinder?=null
}