package com.pausebite.poc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
 private lateinit var status: TextView
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,24) }
  root.addView(TextView(this).apply { text="PauseBite — Overlay-only Test"; textSize=24f })
  root.addView(TextView(this).apply { text="This safe-scope test does not monitor other apps. It only checks whether a user-triggered popup can appear above apps."; textSize=16f })
  status=TextView(this).apply { textSize=16f; setPadding(0,20,0,20) }
  root.addView(status)
  root.addView(Button(this).apply {
   text="1. Allow display over other apps"
   setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) }
  })
  root.addView(Button(this).apply {
   text="2. Show popup, then open another app"
   setOnClickListener {
    if(!Settings.canDrawOverlays(this@MainActivity)) {
     Toast.makeText(this@MainActivity,"Grant overlay permission first",Toast.LENGTH_LONG).show()
    } else {
     Overlay.show(this@MainActivity,"Manual overlay test")
     Toast.makeText(this@MainActivity,"Press Home and open another app; dismiss popup there",Toast.LENGTH_LONG).show()
    }
   }
  })
  root.addView(Button(this).apply { text="Dismiss popup"; setOnClickListener { Overlay.hide() } })
  setContentView(root)
 }
 override fun onResume() { super.onResume(); if(::status.isInitialized) status.text="Overlay permission: ${Settings.canDrawOverlays(this)}" }
}