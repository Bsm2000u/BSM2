package com.bsm.a51onelauncher

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.abs

/**
 * Owns the home screen's scrollable surface and swipe-up gesture.
 * MainActivity supplies content and actions; this class owns the screen shell.
 */
class HomeScreenView(
    private val activity: Activity,
    wallpaper: Drawable,
    onSwipeUp: () -> Unit,
    onLongPress: () -> Unit
) : FrameLayout(activity) {
    val content = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(48), dp(22), dp(22))
    }

    private var downX = 0f
    private var downY = 0f
    private var detected = false

    init {
        background = wallpaper
        isClickable = true
        setOnLongClickListener { onLongPress(); true }

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content, LayoutParams(-1, -1))
        }
        addView(scroll, LayoutParams(-1, -1))
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                detected = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (dy < -dp(70) && abs(dy) > abs(dx) * 1.25f) {
                    detected = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP && detected) {
            detected = false
            activity.runOnUiThread { onSwipeUpAction?.invoke() }
            return true
        }
        return true
    }

    private var onSwipeUpAction: (() -> Unit)? = onSwipeUp

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
