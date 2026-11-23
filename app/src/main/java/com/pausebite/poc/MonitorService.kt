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

class MonitorService:Service() {
 private val handler=Handler(Looper.getMainLooper())
 private val targets=mapOf("com.application.zomato" to "Zomato","in.swiggy.android" to "Swiggy")
 private var lastForeground=""
 private var lastEventTime=System.currentTimeMillis()-1500L
 private val tick=object:Runnable {
  override fun run() { try { poll() } catch(e:Exception) { Log.e("PauseBite","Polling error",e) }; handler.postDelayed(this,750) }
 }
 override fun onCreate() { super.onCreate(); startNotification(); handler.post(tick) }
 private fun startNotification() {
  val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel("monitor","PauseBite monitoring",NotificationManager.IMPORTANCE_LOW))
  val stop=PendingIntent.getService(this,1,Intent(this,MonitorService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notification=Notification.Builder(this,"monitor").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("PauseBite monitoring active").setContentText("Watching selected food apps").addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
  if(Build.VERSION.SDK_INT>=34) startForeground(42,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(42,notification)
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(intent?.action=="STOP") { stopSelf(); return START_NOT_STICKY }
  return START_NOT_STICKY
 }
 private fun poll() {
  val now=System.currentTimeMillis()
  val events=(getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager).queryEvents(lastEventTime,now)
  val event=UsageEvents.Event()
  var newest=lastEventTime
  var pkg:String?=null
  while(events.hasNextEvent()) {
   events.getNextEvent(event)
   if(event.eventType==UsageEvents.Event.ACTIVITY_RESUMED || event.eventType==UsageEvents.Event.MOVE_TO_FOREGROUND) {
    if(event.timeStamp>=newest) { newest=event.timeStamp; pkg=event.packageName }
   }
  }
  lastEventTime=now-1500L
  if(pkg!=null && pkg!=lastForeground) {
   lastForeground=pkg!!
   val name=targets[pkg]
   if(name!=null) {
    val snooze=getSharedPreferences("poc",MODE_PRIVATE).getLong("snooze_until",0)
    if(now>=snooze) { Log.i("PauseBite","Detected $name; event age=${now-newest}ms"); Overlay.show(this,name) }
   } else Overlay.hide()
  }
 }
 override fun onDestroy() { handler.removeCallbacks(tick); Overlay.hide(); super.onDestroy() }
 override fun onBind(intent:Intent?):IBinder?=null
}