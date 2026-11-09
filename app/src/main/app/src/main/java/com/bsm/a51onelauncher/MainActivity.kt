package com.bsm.a51onelauncher

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(245, 245, 250)
        window.navigationBarColor = Color.rgb(245, 245, 250)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(245, 245, 250))
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "A51 One Launcher"
            textSize = 28f
            setTextColor(Color.rgb(30, 35, 50))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Welcome to your new home"
            textSize = 15f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        layout.addView(title)
        layout.addView(subtitle)

        setContentView(layout)
    }
}
