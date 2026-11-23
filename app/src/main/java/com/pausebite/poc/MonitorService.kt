package com.pausebite.poc

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log

class MonitorService : Service() {
 private val handler = Handler(Looper.getMainLooper())
 private val targets = mapOf("com.application.zomato" to "Zomato", "in.swiggy.android" to "Swiggy")
 private lateinit var power: PowerManager
 private var lastForeground = ""
 private var lastEventAt = System.currentTimeMillis()
 private var lastEventKey = ""
 private var fastUntil = 0L
 private var pollCount = 0L
 private var startedAt = 0L
 private var receiverRegistered = false
 private var monitoringStarted = false
 private var batteryLevel = -1
 private var lowBattery = false
 private var lastBatteryRead = 0L
 private val screenReceiver = object : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
   when (intent?.action) {
    Intent.ACTION_SCREEN_OFF -> {
     handler.removeCallbacks(tick)
     Overlay.hide()
     saveState("Paused: screen off", 0L)
    }
    Intent.ACTION_SCREEN_ON -> {
     // Screen-on events may arrive before the device is fully interactive.
     handler.removeCallbacks(tick)
     handler.postDelayed(tick, 600L)
    }
    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED, Intent.ACTION_BATTERY_LOW, Intent.ACTION_BATTERY_OKAY -> {
     readBattery(true)
     handler.removeCallbacks(tick)
     if (power.isInteractive) handler.post(tick)
    }
   }
  }
 }
 private val tick = object : Runnable {
  override fun run() {
   if (!power.isInteractive) {
    saveState("Paused: screen off", 0L)
    return
   }
   readBattery(false)
   try { poll() } catch (e: Exception) {
    getSharedPreferences("poc", MODE_PRIVATE).edit().putString("last_reason", "Monitor error: ${e.javaClass.simpleName}").apply()
    Log.e("PauseBite", "Monitoring error", e)
   }
   pollCount++
   val interval = when {
    power.isPowerSaveMode || lowBattery -> 5000L
    SystemClock.elapsedRealtime() < fastUntil -> 900L
    else -> 1800L
   }
   if (pollCount % 20L == 0L) saveState(null, interval)
   handler.postDelayed(this, interval)
  }
 }
 override fun onCreate() {
  super.onCreate()
  power = getSystemService(POWER_SERVICE) as PowerManager
  startedAt = System.currentTimeMillis()
  lastEventAt = startedAt - 3000L
  readBattery(true)
  val filter = IntentFilter().apply {
   addAction(Intent.ACTION_SCREEN_ON)
   addAction(Intent.ACTION_SCREEN_OFF)
   addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
   addAction(Intent.ACTION_BATTERY_LOW)
   addAction(Intent.ACTION_BATTERY_OKAY)
  }
  if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
  else registerReceiver(screenReceiver, filter)
  receiverRegistered = true
 }
 private fun readBattery(force: Boolean) {
  val now = SystemClock.elapsedRealtime()
  if (!force && now - lastBatteryRead < 60000L) return
  lastBatteryRead = now
  val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
  val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
  val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
  batteryLevel = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
  val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
  val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
  lowBattery = batteryLevel in 0..19 && !charging
  getSharedPreferences("poc", MODE_PRIVATE).edit()
   .putInt("battery_level", batteryLevel).putBoolean("battery_low", lowBattery)
   .putBoolean("power_save", power.isPowerSaveMode).apply()
 }
 private fun saveState(reason: String?, interval: Long) {
  val edit = getSharedPreferences("poc", MODE_PRIVATE).edit()
   .putLong("poll_count", pollCount).putLong("poll_interval_ms", interval)
   .putBoolean("screen_active", power.isInteractive)
  if (reason != null) edit.putString("monitor_state", reason)
  else edit.putString("monitor_state", when {
   power.isPowerSaveMode || lowBattery -> "Eco: battery protection"
   SystemClock.elapsedRealtime() < fastUntil -> "Responsive: recent food app"
   else -> "Balanced monitoring"
  })
  edit.apply()
 }
 private fun startNotification() {
  val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel("monitor", "PauseBite monitoring", NotificationManager.IMPORTANCE_LOW))
  val stop = PendingIntent.getService(this, 1, Intent(this, MonitorService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notification = Notification.Builder(this, "monitor")
   .setSmallIcon(android.R.drawable.ic_dialog_info)
   .setContentTitle("PauseBite monitoring active")
   .setContentText("Battery-aware · pauses when screen is off")
   .setOngoing(true)
   .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
  if (Build.VERSION.SDK_INT >= 34) startForeground(42, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
  else startForeground(42, notification)
 }
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
  if (!monitoringStarted) {
   monitoringStarted = true
   startNotification()
   getSharedPreferences("poc", MODE_PRIVATE).edit().putBoolean("monitoring", true)
    .putLong("monitor_started_at", startedAt).putString("last_reason", "Monitoring started").apply()
   if (power.isInteractive) handler.post(tick) else saveState("Paused: screen off", 0L)
  }
  return START_NOT_STICKY
 }
 private fun poll() {
  val now = System.currentTimeMillis()
  // Bound the lookback after sleep/clock changes; overlapping events are deduplicated.
  val from = (lastEventAt - 2500L).coerceAtLeast(now - 12000L).coerceAtLeast(0L)
  val events = (getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager).queryEvents(from, now)
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
  if (lastForeground == pkg) return
  lastForeground = pkg
  fastUntil = SystemClock.elapsedRealtime() + 15000L
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
  // Do not write foreground app names to system logs; diagnostics stay in private app storage.
 }
 override fun onDestroy() {
  handler.removeCallbacks(tick)
  if (receiverRegistered) { unregisterReceiver(screenReceiver); receiverRegistered = false }
  Overlay.hide()
  saveState("Monitoring stopped", 0L)
  getSharedPreferences("poc", MODE_PRIVATE).edit().putBoolean("monitoring", false)
   .putString("last_reason", "Monitoring stopped").apply()
  super.onDestroy()
 }
 override fun onBind(intent: Intent?): IBinder? = null
}
