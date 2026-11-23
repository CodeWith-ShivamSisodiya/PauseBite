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
import android.widget.TextView
import android.widget.Toast

class MainActivity: Activity() {
 private lateinit var status: TextView
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,24) }
  fun addButton(label:String, action:()->Unit) { root.addView(Button(this).apply { text=label; setOnClickListener { action() } }) }
  root.addView(TextView(this).apply { text="PauseBite · Overlay POC"; textSize=24f })
  root.addView(TextView(this).apply { text="Opt-in monitoring for Zomato and Swiggy. Usage information remains on-device."; textSize=16f })
  status=TextView(this).apply { textSize=16f; setPadding(0,20,0,20) }; root.addView(status)
  addButton("1. Grant Usage Access") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
  addButton("2. Grant Overlay Permission") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
  if(Build.VERSION.SDK_INT>=33) addButton("3. Allow Notifications") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),8) }
  addButton("START monitoring") {
   if(!usageAllowed() || !Settings.canDrawOverlays(this)) { Toast.makeText(this,"Grant both special permissions first",Toast.LENGTH_LONG).show() }
   else try { startForegroundService(Intent(this,MonitorService::class.java)); Toast.makeText(this,"Monitoring started",Toast.LENGTH_SHORT).show() }
   catch(e:Exception) { Toast.makeText(this,"Start failed: ${e.message}",Toast.LENGTH_LONG).show() }
  }
  addButton("STOP monitoring") { stopService(Intent(this,MonitorService::class.java)) }
  addButton("Show test popup now") { if(Settings.canDrawOverlays(this)) Overlay.show(this,"Manual test") else Toast.makeText(this,"Grant overlay permission first",Toast.LENGTH_SHORT).show() }
  setContentView(root)
 }
 override fun onResume() { super.onResume(); if(::status.isInitialized) status.text="Usage Access: ${usageAllowed()} | Overlay: ${Settings.canDrawOverlays(this)}" }
 private fun usageAllowed():Boolean {
  val ops=getSystemService(APP_OPS_SERVICE) as AppOpsManager
  return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),packageName)==AppOpsManager.MODE_ALLOWED
 }
}