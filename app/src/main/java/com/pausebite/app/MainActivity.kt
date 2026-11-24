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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

@SuppressLint("SetTextI18n", "ImplicitSamInstance")
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var diagnostics: TextView
    private lateinit var zomatoSwitch: Switch
    private lateinit var swiggySwitch: Switch

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
            text = "PauseBite works offline. You choose which food apps it may notice. Its small, clearly branded Pause Card never reads the food app and does not block the rest of the screen. No app-usage data is uploaded or shared. Monitoring starts only when you tap START and stops when you tap STOP."
            textSize = 15f
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 18, 0, 18)
        }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "Choose apps for mindful reminders"
            textSize = 17f
            setPadding(0, 8, 0, 4)
        })

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        zomatoSwitch = Switch(this).apply {
            text = "Zomato"
            isChecked = prefs.getBoolean("target_zomato", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("target_zomato", checked).apply()
                if (!checked && !hasSelectedTargets()) {
                    stopService(Intent(this@MainActivity, MonitorService::class.java))
                }
                refresh()
            }
        }
        root.addView(zomatoSwitch)
        swiggySwitch = Switch(this).apply {
            text = "Swiggy"
            isChecked = prefs.getBoolean("target_swiggy", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("target_swiggy", checked).apply()
                if (!checked && !hasSelectedTargets()) {
                    stopService(Intent(this@MainActivity, MonitorService::class.java))
                }
                refresh()
            }
        }
        root.addView(swiggySwitch)

        fun button(title: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = title
                setOnClickListener { action() }
            })
        }

        button("1. Grant Usage Access") { showUsageDisclosure() }
        button("2. Allow PauseBite overlay") { showOverlayDisclosure() }
        button("3. Allow monitoring notification") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
            } else {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        }
        button("START monitoring") {
            when {
                !hasSelectedTargets() -> toast("Select Zomato, Swiggy, or both first")
                !usageAllowed() -> toast("Grant Usage Access first")
                !overlayAllowed() -> toast("Allow PauseBite to display over other apps")
                !notificationsAllowed() -> toast("Allow the monitoring notification first")
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
        button("Preview the transparent Pause Card") {
            if (!overlayAllowed()) {
                toast("Allow the PauseBite overlay first")
            } else if (!notificationsAllowed()) {
                toast("Allow the monitoring notification first")
            } else {
                startForegroundService(
                    Intent(this, MonitorService::class.java).setAction(MonitorService.ACTION_TEST_OVERLAY)
                )
            }
        }
        button("Refresh diagnostics") { refresh() }
        button("Privacy policy") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
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
        val selected = buildList {
            if (prefs.getBoolean("target_zomato", false)) add("Zomato")
            if (prefs.getBoolean("target_swiggy", false)) add("Swiggy")
        }.ifEmpty { listOf("None") }.joinToString()
        status.text = "Selected apps: $selected\nUsage Access: ${usageAllowed()} | Overlay: ${overlayAllowed()} | Notification: ${notificationsAllowed()} | Monitoring: ${prefs.getBoolean("monitoring", false)}"
        if (::diagnostics.isInitialized) {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            diagnostics.text = "Detected openings: ${prefs.getInt("detected_count", 0)}\n" +
                "Pause Cards shown: ${prefs.getInt("overlay_count", 0)}\n" +
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
                "While monitoring is switched on, PauseBite reads Android foreground-app transition events on this device to detect only the food apps you selected. Other app events may be returned by Android, but PauseBite immediately ignores them and does not save their history. Detection, cooldowns, and aggregate counters stay on this device. PauseBite has no Internet permission and does not upload, sell, or share app-activity data. You can stop monitoring at any time."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .show()
    }

    private fun showOverlayDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("How the Pause Card works")
            .setMessage(
                "When monitoring is on and you open a food-delivery app that you selected, PauseBite uses Android's Display over other apps access to show one small, clearly branded card. It does not cover the whole screen; content and controls outside the card remain visible and touchable. The card cannot read, capture, or modify the food app. Close it immediately, dismiss it for today, or stop monitoring directly from the card."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open settings") { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .show()
    }

    private fun overlayAllowed(): Boolean = Settings.canDrawOverlays(this)

    private fun hasSelectedTargets(): Boolean {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        return prefs.getBoolean("target_zomato", false) || prefs.getBoolean("target_swiggy", false)
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
