package com.pausebite.app

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OverlayController(
    context: Context,
    private val onAction: (Action) -> Unit
) {
    enum class Action {
        CONTINUE,
        DISMISS_TODAY,
        STOP_MONITORING
    }

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    fun show(source: String): Boolean {
        if (!Settings.canDrawOverlays(appContext)) return false
        dismiss()

        val card = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(20), dp(22), dp(22))
            background = rounded(Color.rgb(248, 246, 239), 28, GREEN)
            elevation = dp(16).toFloat()
            isClickable = true
            contentDescription = appContext.getString(R.string.overlay_content_description, source)
        }

        val top = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(appContext).apply {
            text = appContext.getString(R.string.overlay_brand)
            setTextColor(GREEN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Button(appContext).apply {
            text = appContext.getString(R.string.overlay_close_symbol)
            contentDescription = appContext.getString(R.string.overlay_close_description)
            isAllCaps = false
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            background = rounded(Color.TRANSPARENT, 12)
            minWidth = dp(48)
            minHeight = dp(48)
            setOnClickListener { finish(Action.CONTINUE) }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        card.addView(top, matchWidth())

        card.addView(TextView(appContext).apply {
            text = appContext.getString(R.string.overlay_heading)
            setTextColor(INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, matchWidth(topMargin = 16))

        card.addView(TextView(appContext).apply {
            text = appContext.getString(R.string.overlay_body, source)
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
        }, matchWidth(topMargin = 10))

        card.addView(TextView(appContext).apply {
            text = appContext.getString(R.string.overlay_privacy_note)
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
        }, matchWidth(topMargin = 8))

        card.addView(TextView(appContext).apply {
            text = "5"
            setTextColor(GREEN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(228, 243, 236), 52)
        }, LinearLayout.LayoutParams(dp(104), dp(104)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(24)
            bottomMargin = dp(22)
        })

        val moods = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf(R.string.overlay_mood_hungry, R.string.overlay_mood_craving, R.string.overlay_mood_stressed)
            .forEachIndexed { index, label ->
            moods.addView(TextView(appContext).apply {
                text = appContext.getString(label)
                setTextColor(INK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                background = rounded(Color.WHITE, 14, Color.rgb(214, 225, 220))
                setPadding(dp(4), dp(14), dp(4), dp(14))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (index > 0) marginStart = dp(7)
            })
        }
        card.addView(moods, matchWidth())

        card.addView(actionButton(appContext.getString(R.string.overlay_continue), primary = true) {
            finish(Action.CONTINUE)
        }, matchWidth(topMargin = 20))
        card.addView(actionButton(appContext.getString(R.string.overlay_dismiss_today), primary = false) {
            finish(Action.DISMISS_TODAY)
        }, matchWidth(topMargin = 9))
        card.addView(actionButton(appContext.getString(R.string.overlay_stop_monitoring), primary = false) {
            finish(Action.STOP_MONITORING)
        }, matchWidth(topMargin = 9))

        val windowWidth = (appContext.resources.displayMetrics.widthPixels - dp(32))
            .coerceAtMost(dp(440))

        val params = WindowManager.LayoutParams(
            windowWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            setTitle("PauseBite user-enabled mindful pause")
        }

        return try {
            windowManager.addView(card, params)
            overlayView = card
            true
        } catch (_: Exception) {
            overlayView = null
            false
        }
    }

    fun dismiss() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // The system may already have detached the overlay.
        }
    }

    private fun finish(action: Action) {
        dismiss()
        onAction(action)
    }

    private fun actionButton(label: String, primary: Boolean, action: () -> Unit) = Button(appContext).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        minHeight = dp(52)
        setTextColor(if (primary) Color.WHITE else MUTED)
        background = if (primary) {
            rounded(GREEN, 15)
        } else {
            rounded(Color.TRANSPARENT, 15, Color.rgb(205, 217, 212))
        }
        setOnClickListener { action() }
    }

    private fun matchWidth(topMargin: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { this.topMargin = dp(topMargin) }

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int? = null) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) setStroke(dp(1), strokeColor)
    }

    private fun dp(value: Int): Int = (value * appContext.resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        val GREEN = Color.rgb(20, 118, 91)
        val INK = Color.rgb(24, 50, 44)
        val MUTED = Color.rgb(94, 113, 106)
    }
}
