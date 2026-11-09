package com.bsm.a51onelauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : Activity() {
    private val ink = Color.rgb(27, 31, 41)
    private val muted = Color.rgb(112, 118, 131)
    private val canvas = Color.rgb(246, 247, 251)
    private val apps = mutableListOf<LauncherApp>()
    private var drawerDialog: AlertDialog? = null
    private var drawerAdapter: AppGridAdapter? = null
    private var drawerSearch: EditText? = null

    private data class LauncherApp(val info: ResolveInfo, val title: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        showLoading()
        loadAppsInBackground()
    }

    private fun wallpaperDrawable(): Drawable {
        return try {
            WallpaperManager.getInstance(this).drawable ?: ColorDrawable(Color.DKGRAY)
        } catch (_: Exception) {
            ColorDrawable(Color.DKGRAY)
        }
    }

    private fun showLoading() {
        val view = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            background = wallpaperDrawable()
            addView(TextView(this@MainActivity).apply {
                text = "One UI Launcher"
                textSize = 22f
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setShadowLayer(5f, 0f, 2f, Color.BLACK)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Preparing your apps…"
                textSize = 13f
                setTextColor(Color.WHITE)
                setPadding(0, dp(8), 0, 0)
                setShadowLayer(4f, 0f, 1f, Color.BLACK)
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

    private fun greetingText(): String {
        val hour = SimpleDateFormat("H", Locale.getDefault()).format(Date()).toIntOrNull() ?: 12
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    private inner class SwipeHomeLayout(context: Activity, private val onSwipeUp: () -> Unit) :
        FrameLayout(context) {
        private var downX = 0f
        private var downY = 0f
        private var detected = false

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
                onSwipeUp()
                return true
            }
            return true
        }
    }

    private fun showHome() {
        val root = SwipeHomeLayout(this) { showAppDrawer() }.apply {
            background = wallpaperDrawable()
            isClickable = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(48), dp(22), dp(24))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
        header.addView(label(
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            15f, Color.WHITE, true
        ).apply { setShadowLayer(5f, 0f, 2f, Color.BLACK) })
        header.addView(label(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()), 58f, Color.WHITE, true).apply {
            setPadding(0, dp(2), 0, 0)
            setShadowLayer(7f, 0f, 3f, Color.BLACK)
        })
        header.addView(label(greetingText(), 14f, Color.WHITE).apply {
            setPadding(0, dp(1), 0, 0)
            setShadowLayer(4f, 0f, 2f, Color.BLACK)
        })
        content.addView(header)

        content.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))

        val grid = GridView(this).apply {
            numColumns = 4
            horizontalSpacing = dp(8)
            verticalSpacing = dp(8)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
            adapter = AppGridAdapter(apps.take(8))
            layoutParams = LinearLayout.LayoutParams(-1, dp(210))
            selector = ColorDrawable(Color.TRANSPARENT)
        }
        content.addView(grid)

        val search = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.argb(205, 255, 255, 255), dp(28).toFloat())
            setPadding(dp(18), 0, dp(18), 0)
            isClickable = true
            isFocusable = true
            elevation = dp(3).toFloat()
        }
        search.addView(label("⌕", 25f, Color.DKGRAY, true))
        search.addView(label("Search apps", 15f, Color.DKGRAY).apply {
            setPadding(dp(12), dp(15), 0, dp(15))
        })
        search.setOnClickListener { showAppDrawer() }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(15)
        })

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.argb(190, 245, 247, 252), dp(30).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            elevation = dp(5).toFloat()
        }
        val dockApps = apps.take(4)
        dockApps.forEach { app ->
            val slot = FrameLayout(this).apply {
                isClickable = true
                isFocusable = true
                setOnClickListener { launch(app.info) }
            }
            val icon = ImageView(this).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            slot.addView(icon, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER))
            dock.addView(slot, LinearLayout.LayoutParams(0, dp(54), 1f))
        }
        content.addView(dock, LinearLayout.LayoutParams(-1, dp(74)).apply {
            topMargin = dp(16)
        })

        root.addView(content, FrameLayout.LayoutParams(-1, -1))
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
                tile.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
                name = label("", 11f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setShadowLayer(4f, 0f, 1f, Color.BLACK)
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
        if (drawerDialog != null) {
            drawerSearch?.setText("")
            drawerDialog?.show()
            return
        }
        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(42), dp(20), dp(12))
            background = wallpaperDrawable()
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(label("All apps", 27f, Color.WHITE, true).apply {
            setShadowLayer(5f, 0f, 2f, Color.BLACK)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(label(apps.size.toString() + " apps", 12f, Color.WHITE).apply {
            setShadowLayer(4f, 0f, 1f, Color.BLACK)
        })
        dialogContent.addView(heading)

        val search = EditText(this).apply {
            hint = "Search apps"
            isSingleLine = true
            textSize = 15f
            setTextColor(ink)
            setHintTextColor(muted)
            background = rounded(Color.argb(220, 255, 255, 255), dp(22).toFloat())
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
            selector = ColorDrawable(Color.TRANSPARENT)
        }
        drawerAdapter = AppGridAdapter(apps)
        grid.adapter = drawerAdapter
        drawerSearch = search
        dialogContent.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        val dialog = AlertDialog.Builder(this).setView(dialogContent).setNegativeButton("Close", null).create()
        drawerDialog = dialog
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                drawerAdapter?.submit(if (query.isEmpty()) apps else apps.filter { it.title.contains(query, true) })
                grid.setSelection(0)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setDimAmount(0.08f)
            dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }
}
