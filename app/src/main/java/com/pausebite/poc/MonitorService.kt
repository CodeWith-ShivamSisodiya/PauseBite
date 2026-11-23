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
 private val handler = Handler(Looper.getMainLooper())
 private val targets = mapOf("com.application.zomato" to "Zomato", "in.swiggy.android" to "Swiggy")
 private var lastForeground = ""
 private var lastEventAt = System.currentTimeMillis()
 private var lastEventKey = ""
 private val tick = object : Runnable {
  override fun run() {
   try { poll() } catch (e: Exception) {
    getSharedPreferences("poc", MODE_PRIVATE).edit().putString("last_reason", "Monitor error: ${e.javaClass.simpleName}").apply()
    Log.e("PauseBite", "Monitoring error", e)
   }
   handler.postDelayed(this, 650L)
  }
 }
 override fun onCreate() {
  super.onCreate()
  getSharedPreferences("poc", MODE_PRIVATE).edit().putBoolean("monitoring", true).putString("last_reason", "Monitoring started").apply()
  startNotification()
  handler.post(tick)
 }
 private fun startNotification() {
  val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel("monitor", "PauseBite monitoring", NotificationManager.IMPORTANCE_LOW))
  val stop = PendingIntent.getService(this, 1, Intent(this, MonitorService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notification = Notification.Builder(this, "monitor")
   .setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle("PauseBite monitoring active")
   .setContentText("Swiggy and Zomato · Stop anytime")
   .setOngoing(true)
   .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
  if (Build.VERSION.SDK_INT >= 34) startForeground(42, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
  else startForeground(42, notification)
 }
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
  return START_NOT_STICKY
 }
 private fun poll() {
  val now = System.currentTimeMillis()
  // Overlap the query to tolerate event publication delay; deduplicate by event key.
  val events = (getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager).queryEvents((lastEventAt - 2500L).coerceAtLeast(0L), now)
  val event = UsageEvents.Event()
  var newestTime = lastEventAt
  var newestKey = lastEventKey
  var newestPackage: String? = null
  while (events.hasNextEvent()) {
   events.getNextEvent(event)
   if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
   val key = "${event.timeStamp}:${event.packageName}:${event.className}"
   if (event.timeStamp > newestTime || (event.timeStamp == newestTime && key != newestKey)) {
    newestTime = event.timeStamp
    newestKey = key
    newestPackage = event.packageName
   }
  }
  if (newestPackage == null) return
  lastEventAt = newestTime
  lastEventKey = newestKey
  val pkg = newestPackage
  val name = targets[pkg]
  if (name == null) {
   if (lastForeground != pkg) { lastForeground = pkg; Overlay.hide() }
   return
  }
  // Ignore internal activity transitions in the same food app.
  if (lastForeground == pkg) return
  lastForeground = pkg
  val prefs = getSharedPreferences("poc", MODE_PRIVATE)
  val delay = (now - newestTime).coerceAtLeast(0L)
  val attempts = prefs.getInt("detected_count", 0) + 1
  val snooze = prefs.getLong("snooze_until", 0L)
  val previous = prefs.getLong("last_popup_at", 0L)
  val cooldown = prefs.getLong("cooldown_ms", 5000L)
  val reason = when {
   !android.provider.Settings.canDrawOverlays(this) -> "Overlay permission missing"
   now < snooze -> "Snoozed"
   now - previous < cooldown -> "Cooldown (${(cooldown - (now - previous)).coerceAtLeast(0L) / 1000}s left)"
   Overlay.isShowing() -> "Popup already visible"
   else -> "Popup requested"
  }
  prefs.edit().putString("last_detected", name).putLong("delay_ms", delay)
   .putInt("detected_count", attempts).putString("last_reason", reason).apply()
  if (reason == "Popup requested") {
   if (Overlay.show(this, name)) {
    prefs.edit().putLong("last_popup_at", now)
     .putInt("popup_count", prefs.getInt("popup_count", 0) + 1)
     .putString("last_reason", "Popup shown").apply()
   } else prefs.edit().putString("last_reason", "Overlay display failed").apply()
  }
  Log.i("PauseBite", "Foreground=$name delay=${delay}ms result=${prefs.getString("last_reason", "")}")
 }
 override fun onDestroy() {
  handler.removeCallbacks(tick)
  Overlay.hide()
  getSharedPreferences("poc", MODE_PRIVATE).edit().putBoolean("monitoring", false).putString("last_reason", "Monitoring stopped").apply()
  super.onDestroy()
 }
 override fun onBind(intent: Intent?): IBinder? = null
}