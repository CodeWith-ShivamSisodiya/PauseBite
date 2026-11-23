package com.pausebite.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val targets = mapOf(
        "com.application.zomato" to "Zomato",
        "in.swiggy.android" to "Swiggy"
    )
    private lateinit var powerManager: PowerManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var overlayController: OverlayController
    private var lastForeground = ""
    private var lastEventAt = System.currentTimeMillis()
    private var lastEventKey = ""
    private var fastUntil = 0L
    private var pollCount = 0L
    private var receiverRegistered = false
    private var monitoringStarted = false
    private var lowBattery = false
    private var lastBatteryRead = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    handler.removeCallbacks(tick)
                    overlayController.dismiss()
                    saveState("Paused: screen off", 0L)
                }
                Intent.ACTION_SCREEN_ON -> {
                    handler.removeCallbacks(tick)
                    handler.postDelayed(tick, 600L)
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                Intent.ACTION_BATTERY_LOW,
                Intent.ACTION_BATTERY_OKAY -> {
                    readBattery(true)
                    handler.removeCallbacks(tick)
                    if (powerManager.isInteractive) handler.post(tick)
                }
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!powerManager.isInteractive) {
                saveState("Paused: screen off", 0L)
                return
            }
            readBattery(false)
            try {
                poll()
            } catch (error: Exception) {
                preferences().edit().putString("last_reason", "Monitor error: ${error.javaClass.simpleName}").apply()
                Log.e(LOG_TAG, "Monitoring error", error)
            }
            pollCount++
            val interval = when {
                powerManager.isPowerSaveMode || lowBattery -> 5_000L
                SystemClock.elapsedRealtime() < fastUntil -> 900L
                else -> 1_800L
            }
            if (pollCount % 20L == 0L) saveState(null, interval)
            handler.postDelayed(this, interval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        overlayController = OverlayController(this) { action ->
            when (action) {
                OverlayController.Action.CONTINUE -> {
                    preferences().edit().putString("last_reason", "Pause Card closed; ordering continued").apply()
                }
                OverlayController.Action.DISMISS_TODAY -> {
                    val tomorrow = LocalDate.now().plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    preferences().edit()
                        .putLong("snooze_until", tomorrow)
                        .putString("last_reason", "Pause Cards dismissed until tomorrow")
                        .apply()
                }
            }
        }
        createNotificationChannels()
        lastEventAt = System.currentTimeMillis() - 3_000L
        readBattery(true)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!monitoringStarted) {
            monitoringStarted = true
            startMonitoringNotification()
            preferences().edit()
                .putBoolean("monitoring", true)
                .putLong("monitor_started_at", System.currentTimeMillis())
                .putString("last_reason", "Monitoring started")
                .apply()
            if (powerManager.isInteractive) handler.post(tick) else saveState("Paused: screen off", 0L)
        }

        if (intent?.action == ACTION_TEST_OVERLAY) {
            showPauseCard("Test food app")
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannels() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                MONITOR_CHANNEL_ID,
                "Monitoring status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when user-started PauseBite monitoring is active"
                setShowBadge(false)
            }
        )
    }

    private fun startMonitoringNotification() {
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = servicePendingIntent(2, ACTION_STOP)
        val notification = Notification.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("PauseBite monitoring active")
            .setContentText("Offline monitoring; pauses while the screen is off")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, notification)
        }
    }

    private fun showPauseCard(source: String): Boolean {
        if (!Settings.canDrawOverlays(this)) return false
        return overlayController.show(source)
    }

    private fun servicePendingIntent(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, MonitorService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun poll() {
        val now = System.currentTimeMillis()
        val from = (lastEventAt - 2_500L).coerceAtLeast(now - 12_000L).coerceAtLeast(0L)
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

        val packageName = newestPackage ?: return
        lastEventAt = newestTime
        lastEventKey = newestKey
        val targetName = targets[packageName]
        if (targetName == null) {
            lastForeground = packageName
            return
        }
        if (lastForeground == packageName) return
        lastForeground = packageName
        fastUntil = SystemClock.elapsedRealtime() + 15_000L

        val prefs = preferences()
        val snoozeUntil = prefs.getLong("snooze_until", 0L)
        val previousOverlay = prefs.getLong("last_overlay_at", 0L)
        val cooldown = prefs.getLong("cooldown_ms", 1_800_000L)
        val reason = when {
            now < snoozeUntil -> "Pause Cards dismissed until tomorrow"
            now - previousOverlay < cooldown -> "Cooldown (${(cooldown - (now - previousOverlay)).coerceAtLeast(0L) / 1000}s left)"
            else -> "Pause Card requested"
        }

        prefs.edit()
            .putString("last_detected", targetName)
            .putLong("delay_ms", (now - newestTime).coerceAtLeast(0L))
            .putInt("detected_count", prefs.getInt("detected_count", 0) + 1)
            .putString("last_reason", reason)
            .apply()

        if (reason == "Pause Card requested") {
            if (showPauseCard(targetName)) {
                prefs.edit()
                    .putLong("last_overlay_at", now)
                    .putInt("overlay_count", prefs.getInt("overlay_count", 0) + 1)
                    .putString("last_reason", "Pause Card shown")
                    .apply()
            } else {
                prefs.edit().putString("last_reason", "Overlay permission unavailable").apply()
            }
        }
    }

    private fun readBattery(force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastBatteryRead < 60_000L) return
        lastBatteryRead = now
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        lowBattery = percent in 0..19 && !charging
    }

    private fun saveState(reason: String?, interval: Long) {
        val editor = preferences().edit()
            .putLong("poll_count", pollCount)
            .putLong("poll_interval_ms", interval)
            .putBoolean("screen_active", powerManager.isInteractive)
        editor.putString(
            "monitor_state",
            reason ?: when {
                powerManager.isPowerSaveMode || lowBattery -> "Eco: battery protection"
                SystemClock.elapsedRealtime() < fastUntil -> "Responsive: recent food app"
                else -> "Balanced monitoring"
            }
        ).apply()
    }

    private fun preferences() = getSharedPreferences(PREFS, MODE_PRIVATE)

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        overlayController.dismiss()
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        saveState("Monitoring stopped", 0L)
        preferences().edit()
            .putBoolean("monitoring", false)
            .putString("last_reason", "Monitoring stopped")
            .apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_TEST_OVERLAY = "com.pausebite.app.action.TEST_OVERLAY"
        private const val ACTION_STOP = "com.pausebite.app.action.STOP"
        private const val PREFS = "pausebite"
        private const val LOG_TAG = "PauseBite"
        private const val MONITOR_CHANNEL_ID = "monitoring_status_v1"
        private const val MONITOR_NOTIFICATION_ID = 42
    }
}
