package com.pausebite.poc

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
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

class MainActivity : Activity() {
 private lateinit var status: TextView
 private lateinit var log: TextView
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,48,32,24) }
  root.addView(TextView(this).apply { text = "PauseBite — Reliability POC v0.4"; textSize = 23f })
  root.addView(TextView(this).apply { text = "Opt-in, local-only monitoring of Swiggy and Zomato. Popups are dismissible. Stop monitoring at any time."; textSize = 15f })
  status = TextView(this).apply { textSize = 15f; setPadding(0,18,0,18) }; root.addView(status)
  fun button(title: String, action: () -> Unit) { root.addView(Button(this).apply { text = title; setOnClickListener { action() } }) }
  button("1. Grant Usage Access") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
  button("2. Grant Display Over Other Apps") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
  if (Build.VERSION.SDK_INT >= 33) button("3. Allow Monitoring Notification") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),22) }
  button("START monitoring") {
   if (!usageAllowed() || !Settings.canDrawOverlays(this)) Toast.makeText(this,"Grant Usage Access and Overlay permission first",Toast.LENGTH_LONG).show()
   else try { startForegroundService(Intent(this,MonitorService::class.java)); Toast.makeText(this,"Monitoring started",Toast.LENGTH_SHORT).show() }
   catch (e: Exception) { Toast.makeText(this,"Start failed: ${e.message}",Toast.LENGTH_LONG).show() }
  }
  button("STOP monitoring") { stopService(Intent(this,MonitorService::class.java)); Overlay.hide(); refresh() }
  button("Manual overlay test") { if (Settings.canDrawOverlays(this)) Overlay.show(this,"Manual test") }
  button("Refresh diagnostics") { refresh() }
  button("Set 5-second test cooldown") { getSharedPreferences("poc",MODE_PRIVATE).edit().putLong("cooldown_ms",5000L).apply(); refresh() }
  button("Set 30-minute normal cooldown") { getSharedPreferences("poc",MODE_PRIVATE).edit().putLong("cooldown_ms",1800000L).apply(); refresh() }
  log = TextView(this).apply { textSize = 14f; setPadding(0,18,0,0) }; root.addView(log)
  setContentView(ScrollView(this).apply { addView(root) })
 }
 override fun onResume() { super.onResume(); if (::status.isInitialized) refresh() }
 private fun refresh() {
  val p = getSharedPreferences("poc",MODE_PRIVATE)
  status.text = "Usage Access: ${usageAllowed()} | Overlay: ${Settings.canDrawOverlays(this)} | Monitoring: ${p.getBoolean("monitoring",false)}"
  if (::log.isInitialized) log.text = "Detected openings: ${p.getInt("detected_count",0)}\nPopups shown: ${p.getInt("popup_count",0)}\nLast detected: ${p.getString("last_detected","None")}\nEvent delay: ${p.getLong("delay_ms",-1)} ms\nLast result: ${p.getString("last_reason","None")}\nCooldown: ${p.getLong("cooldown_ms",5000L)/1000} seconds\nVersion: ${packageManager.getPackageInfo(packageName,0).versionName}"
 }
 private fun usageAllowed(): Boolean {
  val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
  return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),packageName) == AppOpsManager.MODE_ALLOWED
 }
}