package com.pausebite.poc
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object Overlay {
 private var view: LinearLayout? = null
 private var manager: WindowManager? = null
 fun isShowing(): Boolean = view != null
 fun show(context: Context, source: String): Boolean {
  if (!Settings.canDrawOverlays(context) || view != null) return false
  val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  val d = context.resources.displayMetrics.density
  val panel = LinearLayout(context).apply {
   orientation = LinearLayout.VERTICAL
   setPadding((24*d).toInt(), (22*d).toInt(), (24*d).toInt(), (20*d).toInt())
   background = GradientDrawable().apply { setColor(Color.rgb(255,249,238)); cornerRadius = 24*d }
   elevation = 12*d
  }
  panel.addView(TextView(context).apply { text = "🍔 PauseBite"; textSize = 22f; setTextColor(Color.rgb(40,65,50)); setTypeface(null, Typeface.BOLD) })
  panel.addView(TextView(context).apply { text = "Hungry, or just craving?\nTake five seconds to decide. You can still order."; textSize = 17f; setTextColor(Color.DKGRAY); setPadding(0,16,0,12) })
  panel.addView(TextView(context).apply { text = "Detected: $source"; textSize = 12f; setTextColor(Color.GRAY) })
  panel.addView(Button(context).apply { text = "Continue to food app"; setOnClickListener { hide() } })
  panel.addView(Button(context).apply { text = "Snooze for 30 minutes"; setOnClickListener {
   context.getSharedPreferences("poc", Context.MODE_PRIVATE).edit().putLong("snooze_until", System.currentTimeMillis() + 1800000L).apply()
   hide()
  } })
  val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.BOTTOM }
  return try { wm.addView(panel, params); view = panel; manager = wm; true }
  catch (_: Exception) { view = null; manager = null; false }
 }
 fun hide() {
  val v = view; val wm = manager
  view = null; manager = null
  if (v != null && wm != null) try { wm.removeView(v) } catch (_: Exception) {}
 }
}