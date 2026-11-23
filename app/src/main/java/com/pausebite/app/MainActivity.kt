package com.pausebite.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

@SuppressLint("SetTextI18n", "ImplicitSamInstance")
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var diagnostics: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 24)
        }
        root.addView(TextView(this).apply {
            text = "PauseBite"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "PauseBite works offline. Usage Access is used locally to notice when Swiggy or Zomato opens. It then posts a dismissible mindful-eating reminder notification. No app-usage data is uploaded or shared. Monitoring starts only when you tap START and stops when you tap STOP."
            textSize = 15f
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 18, 0, 18)
        }
        root.addView(status)

        fun button(title: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = title
                setOnClickListener { action() }
            })
        }

        button("1. Grant Usage Access") { showUsageDisclosure() }
        button("2. Allow reminder notifications") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
            } else {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        }
        button("START monitoring") {
            when {
                !usageAllowed() -> toast("Grant Usage Access first")
                !notificationsAllowed() -> toast("Allow notifications so PauseBite can show reminders")
                else -> {
                    try {
                        startForegroundService(Intent(this, MonitorService::class.java))
                        toast("Monitoring started")
                    } catch (error: Exception) {
                        toast("Start failed: ${error.javaClass.simpleName}")
                    }
                }
            }
        }
        button("STOP monitoring") {
            stopService(Intent(this, MonitorService::class.java))
            refresh()
        }
        button("Test reminder notification") {
            if (!notificationsAllowed()) {
                toast("Allow notifications first")
            } else {
                startForegroundService(
                    Intent(this, MonitorService::class.java).setAction(MonitorService.ACTION_TEST_REMINDER)
                )
            }
        }
        button("Refresh diagnostics") { refresh() }
        button("Privacy policy") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        }
        button("Set 5-second test cooldown") {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong("cooldown_ms", 5_000L).apply()
            refresh()
        }
        button("Set 30-minute normal cooldown") {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong("cooldown_ms", 1_800_000L).apply()
            refresh()
        }

        diagnostics = TextView(this).apply {
            textSize = 14f
            setPadding(0, 18, 0, 0)
        }
        root.addView(diagnostics)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refresh()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_REQUEST) refresh()
    }

    private fun refresh() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        status.text = "Usage Access: ${usageAllowed()} | Notifications: ${notificationsAllowed()} | Monitoring: ${prefs.getBoolean("monitoring", false)}"
        if (::diagnostics.isInitialized) {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            diagnostics.text = "Detected openings: ${prefs.getInt("detected_count", 0)}\n" +
                "Reminders shown: ${prefs.getInt("reminder_count", 0)}\n" +
                "Last detected: ${prefs.getString("last_detected", "None")}\n" +
                "Event delay: ${prefs.getLong("delay_ms", -1L)} ms\n" +
                "Last result: ${prefs.getString("last_reason", "None")}\n" +
                "Cooldown: ${prefs.getLong("cooldown_ms", 1_800_000L) / 1000} seconds\n" +
                "Monitor mode: ${prefs.getString("monitor_state", "Starting")}\n" +
                "Screen active: ${prefs.getBoolean("screen_active", true)} | Poll interval: ${prefs.getLong("poll_interval_ms", 0L)} ms\n" +
                "Polling cycles: ${prefs.getLong("poll_count", 0L)}\n" +
                "Version: ${packageInfo.versionName} ($versionCode)"
        }
    }

    private fun usageAllowed(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun showUsageDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("How PauseBite uses app activity")
            .setMessage(
                "While monitoring is switched on, PauseBite reads Android foreground-app transition events on this device to detect only Swiggy and Zomato. Other app events may be returned by Android, but PauseBite does not save a history of them. Detection, cooldowns, and aggregate counters stay on this device. PauseBite has no Internet permission and does not upload, sell, or share app-activity data. You can stop monitoring at any time."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .show()
    }

    private fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).areNotificationsEnabled()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private companion object {
        const val PREFS = "pausebite"
        const val NOTIFICATION_REQUEST = 22
        const val PRIVACY_POLICY_URL =
            "https://github.com/CodeWith-ShivamSisodiya/PauseBite/blob/main/PRIVACY_POLICY.md"
    }
}
