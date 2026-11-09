package com.bsm.a51onelauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val ink = Color.rgb(27, 31, 41)
    private val muted = Color.rgb(112, 118, 131)
    private val canvas = Color.rgb(246, 247, 251)
    private val apps = mutableListOf<LauncherApp>()
    private var drawerDialog: AlertDialog? = null
    private var drawerAdapter: AppGridAdapter? = null

    private data class LauncherApp(val info: ResolveInfo, val title: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        showLoading()
        loadAppsInBackground()
    }

    private fun showLoading() {
        val view = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(canvas)
            addView(TextView(this@MainActivity).apply {
                text = "One UI Launcher"
                textSize = 22f
                setTextColor(ink)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Preparing your apps…"
                textSize = 13f
                setTextColor(muted)
                setPadding(0, dp(8), 0, 0)
            })
        }
        setContentView(view)
    }

    private fun loadAppsInBackground() {
        thread(name = "launcher-app-loader") {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val loaded = packageManager.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map { LauncherApp(it, it.loadLabel(packageManager).toString(), it.loadIcon(packageManager)) }
                .sortedBy { it.title.lowercase(Locale.getDefault()) }
            runOnUiThread {
                apps.clear()
                apps.addAll(loaded)
                showHome()
            }
        }
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

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
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(12))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val greeting = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
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
        clockCard.addView(label(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()), 48f, ink, true))
        clockCard.addView(label(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 14f, muted).apply {
            setPadding(0, dp(2), 0, 0)
        })
        content.addView(clockCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })

        val search = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(235, 237, 243), dp(24).toFloat())
            setPadding(dp(18), 0, dp(18), 0)
            isClickable = true
            isFocusable = true
        }
        search.addView(label("⌕", 25f, muted, true))
        search.addView(label("Search apps", 15f, muted).apply { setPadding(dp(12), dp(15), 0, dp(15)) })
        search.setOnClickListener { showAppDrawer() }
        content.addView(search, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        section.addView(label("Your apps", 19f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        section.addView(label("See all  ›", 13f, Color.rgb(65, 94, 173), true).apply {
            setOnClickListener { showAppDrawer() }
        })
        content.addView(section, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(26); bottomMargin = dp(14)
        })

        val grid = GridView(this).apply {
            numColumns = 4
            horizontalSpacing = dp(8)
            verticalSpacing = dp(8)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
            adapter = AppGridAdapter(apps.take(8))
            layoutParams = LinearLayout.LayoutParams(-1, dp(212))
        }
        content.addView(grid)

        val scroll = android.widget.ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
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

    private inner class AppGridAdapter(initialItems: List<LauncherApp>) : BaseAdapter() {
        private val items = initialItems.toMutableList()
        fun submit(newItems: List<LauncherApp>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): LauncherApp = items[position]
        override fun getItemId(position: Int): Long = items[position].info.activityInfo.hashCode().toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tile: LinearLayout
            val icon: ImageView
            val name: TextView
            if (convertView is LinearLayout && convertView.tag is Pair<*, *>) {
                tile = convertView
                @Suppress("UNCHECKED_CAST")
                val views = tile.tag as Pair<ImageView, TextView>
                icon = views.first
                name = views.second
            } else {
                tile = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(7), dp(3), dp(7))
                    minimumHeight = dp(96)
                    isClickable = true
                    isFocusable = true
                }
                icon = ImageView(this@MainActivity).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                tile.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))
                name = label("", 11f, ink, true).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                tile.addView(name, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
                tile.tag = Pair(icon, name)
            }
            val app = getItem(position)
            icon.setImageDrawable(app.icon)
            name.text = app.title
            tile.setOnClickListener { launch(app.info) }
            return tile
        }
    }

    private fun launch(info: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
        try { startActivity(intent) } catch (_: Exception) { }
    }

    private fun showAppDrawer() {
        if (drawerDialog?.isShowing == true) return
        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(20), dp(12))
            setBackgroundColor(canvas)
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(label("All apps", 27f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(label("${apps.size} apps", 12f, muted))
        dialogContent.addView(heading)

        val search = EditText(this).apply {
            hint = "Search apps"
            isSingleLine = true
            textSize = 15f
            setTextColor(ink)
            setHintTextColor(muted)
            background = rounded(Color.WHITE, dp(20).toFloat())
            setPadding(dp(17), 0, dp(17), 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        }
        dialogContent.addView(search, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(18); bottomMargin = dp(12)
        })

        val grid = GridView(this).apply {
            numColumns = 4
            horizontalSpacing = dp(8)
            verticalSpacing = dp(10)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
        }
        drawerAdapter = AppGridAdapter(apps)
        grid.adapter = drawerAdapter
        dialogContent.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        val dialog = AlertDialog.Builder(this).setView(dialogContent).setNegativeButton("Close", null).create()
        drawerDialog = dialog
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                drawerAdapter?.submit(if (query.isEmpty()) apps else apps.filter { it.title.contains(query, true) })
                grid.smoothScrollToPosition(0)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        dialog.setOnDismissListener {
            drawerDialog = null
            drawerAdapter = null
        }
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(rounded(canvas, dp(28).toFloat()))
            dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            dialogContent.alpha = 0f
            dialogContent.animate().alpha(1f).setDuration(160).start()
        }
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }
}
