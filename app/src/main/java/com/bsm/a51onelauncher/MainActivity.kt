package com.bsm.a51onelauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val ink = Color.rgb(28, 32, 43)
    private val muted = Color.rgb(112, 117, 130)
    private val canvas = Color.rgb(246, 247, 251)
    private val apps = mutableListOf<android.content.pm.ResolveInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        loadApps()
        showHome()
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps.clear()
        apps.addAll(packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) })
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(canvas)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(12))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val greeting = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        greeting.addView(label("MONDAY STYLE", 10f, muted, true))
        greeting.addView(label("Good day", 25f, ink, true).apply { setPadding(0, dp(3), 0, 0) })
        val profile = TextView(this).apply {
            text = "●"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(75, 105, 190))
            background = rounded(Color.rgb(231, 236, 249), dp(22).toFloat())
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        }
        top.addView(greeting)
        top.addView(profile)
        content.addView(top)

        val clockCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, dp(28).toFloat())
            setPadding(dp(22), dp(20), dp(22), dp(20))
            elevation = dp(2).toFloat()
        }
        val time = label(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()), 48f, ink, true)
        val date = label(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 14f, muted)
        clockCard.addView(time)
        clockCard.addView(date.apply { setPadding(0, dp(2), 0, 0) })
        val clockParams = LinearLayout.LayoutParams(-1, -2)
        clockParams.topMargin = dp(24)
        content.addView(clockCard, clockParams)

        val search = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(235, 237, 243), dp(24).toFloat())
            setPadding(dp(18), 0, dp(18), 0)
        }
        search.addView(label("⌕", 25f, muted, true))
        search.addView(label("Search apps", 15f, muted).apply { setPadding(dp(12), dp(15), 0, dp(15)) })
        search.setOnClickListener { showAppDrawer() }
        val searchParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) }
        content.addView(search, searchParams)

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        section.addView(label("Your apps", 19f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        val all = label("See all  ›", 13f, Color.rgb(65, 94, 173), true)
        all.setOnClickListener { showAppDrawer() }
        section.addView(all)
        val sectionParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(26); bottomMargin = dp(14) }
        content.addView(section, sectionParams)

        val grid = GridLayout(this).apply { columnCount = 4; alignmentMode = GridLayout.ALIGN_BOUNDS }
        apps.take(8).forEach { grid.addView(appTile(it), GridLayout.LayoutParams().apply {
            width = 0; height = dp(102); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }) }
        content.addView(grid)

        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(18)) }
        content.addView(spacer)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(10), dp(20), dp(18))
            setBackgroundColor(canvas)
        }
        val drawer = TextView(this).apply {
            text = "  ⠿     All apps     "
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(Color.rgb(48, 57, 79), dp(28).toFloat())
            setPadding(dp(30), dp(15), dp(30), dp(15))
            setOnClickListener { showAppDrawer() }
        }
        bottom.addView(drawer)
        root.addView(bottom)
        setContentView(root)
    }

    private fun appTile(info: android.content.pm.ResolveInfo): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(5), dp(3), dp(5))
            isClickable = true
            isFocusable = true
            background = rounded(Color.WHITE, dp(22).toFloat())
            setOnClickListener { launch(info) }
        }
        val icon = ImageView(this).apply {
            setImageDrawable(info.loadIcon(packageManager))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        item.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
        val name = label(info.loadLabel(packageManager).toString(), 11f, ink, true)
        name.gravity = Gravity.CENTER
        name.maxLines = 1
        name.ellipsize = android.text.TextUtils.TruncateAt.END
        val p = LinearLayout.LayoutParams(-1, -2)
        p.topMargin = dp(7)
        item.addView(name, p)
        return item
    }

    private fun launch(info: android.content.pm.ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
        try { startActivity(intent) } catch (_: Exception) { }
    }

    private fun showAppDrawer() {
        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(8))
        }
        val title = label("All apps", 24f, ink, true)
        dialogContent.addView(title)
        val search = EditText(this).apply {
            hint = "Search apps"
            isSingleLine = true
            textSize = 15f
            background = rounded(Color.rgb(240, 242, 247), dp(18).toFloat())
            setPadding(dp(15), 0, dp(15), 0)
        }
        val searchParams = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(14); bottomMargin = dp(12) }
        dialogContent.addView(search, searchParams)
        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply { columnCount = 3 }
        scroll.addView(grid)
        dialogContent.addView(scroll, LinearLayout.LayoutParams(-1, dp(420)))
        fun populate(filter: String) {
            grid.removeAllViews()
            apps.filter { it.loadLabel(packageManager).toString().contains(filter, true) }.forEach {
                grid.addView(appTile(it), GridLayout.LayoutParams().apply {
                    width = 0; height = dp(104); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                })
            }
        }
        populate("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { populate(s?.toString() ?: "") }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val dialog = AlertDialog.Builder(this).setView(dialogContent).setNegativeButton("Close", null).create()
        dialog.window?.setBackgroundDrawable(rounded(Color.WHITE, dp(28).toFloat()))
        dialog.show()
    }
}
