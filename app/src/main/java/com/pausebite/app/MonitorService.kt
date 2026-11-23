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
import android.util.Log

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val targets = mapOf(
        "com.application.zomato" to "Zomato",
        "in.swiggy.android" to "Swiggy"
    )
    private lateinit var powerManager: PowerManager
    private lateinit var notificationManager: NotificationManager
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
                    notificationManager.cancel(REMINDER_NOTIFICATION_ID)
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
            ACTION_SNOOZE -> {
                preferences().edit()
                    .putLong("snooze_until", System.currentTimeMillis() + SNOOZE_MILLIS)
                    .putString("last_reason", "Snoozed for 30 minutes")
                    .apply()
                notificationManager.cancel(REMINDER_NOTIFICATION_ID)
                if (!monitoringStarted) stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                notificationManager.cancel(REMINDER_NOTIFICATION_ID)
                preferences().edit().putString("last_reason", "Reminder dismissed").apply()
                if (!monitoringStarted) stopSelf()
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

        if (intent?.action == ACTION_TEST_REMINDER) {
            showReminder("Test")
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannels() {
        notificationManager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    MONITOR_CHANNEL_ID,
                    "Monitoring status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows when user-started PauseBite monitoring is active"
                    setShowBadge(false)
                },
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Mindful eating reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Optional reminder shown when a supported food app opens"
                    enableVibration(true)
                }
            )
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

    private fun showReminder(source: String): Boolean {
        val openApp = PendingIntent.getActivity(
            this,
            3,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val reminder = Notification.Builder(this, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pause before ordering?")
            .setContentText("$source opened. Hungry, or just craving? Take five seconds to decide.")
            .setStyle(
                Notification.BigTextStyle().bigText(
                    "$source opened. Hungry, or just craving? Take five seconds to decide. You can still order."
                )
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setTimeoutAfter(20_000L)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .addAction(Notification.Action.Builder(null, "Dismiss", servicePendingIntent(4, ACTION_DISMISS)).build())
            .addAction(Notification.Action.Builder(null, "Snooze 30 min", servicePendingIntent(5, ACTION_SNOOZE)).build())
            .build()
        return try {
            notificationManager.notify(REMINDER_NOTIFICATION_ID, reminder)
            true
        } catch (error: SecurityException) {
            Log.w(LOG_TAG, "Reminder notification permission unavailable")
            false
        }
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
        val previousReminder = prefs.getLong("last_reminder_at", 0L)
        val cooldown = prefs.getLong("cooldown_ms", 1_800_000L)
        val reason = when {
            now < snoozeUntil -> "Snoozed"
            now - previousReminder < cooldown -> "Cooldown (${(cooldown - (now - previousReminder)).coerceAtLeast(0L) / 1000}s left)"
            else -> "Reminder requested"
        }

        prefs.edit()
            .putString("last_detected", targetName)
            .putLong("delay_ms", (now - newestTime).coerceAtLeast(0L))
            .putInt("detected_count", prefs.getInt("detected_count", 0) + 1)
            .putString("last_reason", reason)
            .apply()

        if (reason == "Reminder requested") {
            if (showReminder(targetName)) {
                prefs.edit()
                    .putLong("last_reminder_at", now)
                    .putInt("reminder_count", prefs.getInt("reminder_count", 0) + 1)
                    .putString("last_reason", "Reminder shown")
                    .apply()
            } else {
                prefs.edit().putString("last_reason", "Notification permission unavailable").apply()
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
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        notificationManager.cancel(REMINDER_NOTIFICATION_ID)
        saveState("Monitoring stopped", 0L)
        preferences().edit()
            .putBoolean("monitoring", false)
            .putString("last_reason", "Monitoring stopped")
            .apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_TEST_REMINDER = "com.pausebite.app.action.TEST_REMINDER"
        private const val ACTION_STOP = "com.pausebite.app.action.STOP"
        private const val ACTION_SNOOZE = "com.pausebite.app.action.SNOOZE"
        private const val ACTION_DISMISS = "com.pausebite.app.action.DISMISS"
        private const val PREFS = "pausebite"
        private const val LOG_TAG = "PauseBite"
        private const val MONITOR_CHANNEL_ID = "monitoring_status_v1"
        private const val REMINDER_CHANNEL_ID = "mindful_reminders_v1"
        private const val MONITOR_NOTIFICATION_ID = 42
        private const val REMINDER_NOTIFICATION_ID = 43
        private const val SNOOZE_MILLIS = 30 * 60 * 1000L
    }
}
