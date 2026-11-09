package com.bsm.a51onelauncher

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.abs

/**
 * Home-screen canvas: keeps the real system wallpaper visible, adds a subtle
 * readability scrim, and owns the scroll surface and swipe-up gesture.
 */
class HomeScreenView(
    private val activity: Activity,
    wallpaper: Drawable,
    private val onSwipeUp: () -> Unit,
    onLongPress: () -> Unit
) : FrameLayout(activity) {
    val content = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.TOP
        setPadding(dp(22), dp(54), dp(22), dp(26))
        clipToPadding = false
    }

    private var downX = 0f
    private var downY = 0f
    private var detected = false

    init {
        background = wallpaper
        isClickable = true
        setOnLongClickListener { onLongPress(); true }

        // A light-touch vertical scrim improves white-text contrast without
        // replacing or hiding the user's actual wallpaper.
        addView(View(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.argb(34, 0, 0, 0),
                    Color.TRANSPARENT,
                    Color.argb(22, 0, 0, 0),
                    Color.argb(92, 0, 0, 0)
                )
            )
            isClickable = false
        }, LayoutParams(-1, -1))

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(content, LayoutParams(-1, -1))
            setOnLongClickListener { onLongPress(); true }
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
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                if (detected) {
                    detected = false
                    onSwipeUp()
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> detected = false
        }
        return true
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
