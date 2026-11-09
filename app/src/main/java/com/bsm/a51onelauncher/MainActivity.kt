package com.bsm.a51onelauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
import android.widget.ScrollView
import android.widget.TextView
import android.text.TextUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : Activity() {
    private val ink = Color.rgb(24, 28, 38)
    private val muted = Color.rgb(112, 118, 131)
    private val launcherPrefs by lazy { LauncherPreferences(this) }
    private val apps = mutableListOf<LauncherApp>()
    private var drawerDialog: AlertDialog? = null
    private var drawerAdapter: AppGridAdapter? = null
    private var drawerSearch: EditText? = null
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager
    private var widgetContainer: LinearLayout? = null

    private data class LauncherApp(val info: ResolveInfo, val title: String, val icon: Drawable) {
        val key: String get() = info.activityInfo.packageName + "/" + info.activityInfo.name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, 1024)
        try { appWidgetHost.startListening() } catch (_: Exception) {}
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

    override fun onResume() {
        super.onResume()
        try { appWidgetHost.startListening() } catch (_: Exception) {}
    }

    override fun onPause() {
        try { appWidgetHost.stopListening() } catch (_: Exception) {}
        super.onPause()
    }

    @Deprecated("Deprecated in Android API, required for widget picker result compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_WIDGET) {
            val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (resultCode == Activity.RESULT_OK && id != -1) {
                val info = appWidgetManager.getAppWidgetInfo(id)
                val configure = info?.configure
                if (configure != null) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(configure)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    startActivityForResult(intent, REQUEST_CONFIGURE_WIDGET)
                } else {
                    saveWidget(id)
                }
            } else if (id != -1) appWidgetHost.deleteAppWidgetId(id)
        } else if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (resultCode == Activity.RESULT_OK && id != -1) saveWidget(id)
            else if (id != -1) appWidgetHost.deleteAppWidgetId(id)
        }
    }

    private fun saveWidget(id: Int) {
        val ids = widgetIds().toMutableList()
        if (!ids.contains(id)) ids.add(id)
        launcherPrefs.saveWidgetIds(ids)
        showHome()
    }

    private fun widgetIds(): List<Int> = launcherPrefs.widgetIds()

    private fun wallpaperDrawable(): Drawable = try {
        WallpaperManager.getInstance(this).drawable ?: ColorDrawable(Color.DKGRAY)
    } catch (_: Exception) { ColorDrawable(Color.DKGRAY) }

    private fun showLoading() {
        val view = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            background = wallpaperDrawable()
            addView(label("One UI Launcher", 22f, Color.WHITE, true))
            addView(label("Preparing your apps…", 13f, Color.WHITE).apply { setPadding(0, dp(8), 0, 0) })
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
                launcherPrefs.initializeDefaults(
                    apps.take(8).map { it.key },
                    apps.take(5).map { it.key }
                )
                showHome()
            }
        }
    }

    private fun selectedApps(key: String): List<LauncherApp> {
        val keys = launcherPrefs.selectedAppKeys(key)
        return keys.mapNotNull { k -> apps.firstOrNull { it.key == k } }
    }

    private fun saveSelection(key: String, chosen: List<LauncherApp>) {
        launcherPrefs.saveSelectedAppKeys(key, chosen.map { it.key })
        showHome()
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

    private fun showHome() {
        val home = HomeScreenView(
            activity = this,
            wallpaper = wallpaperDrawable(),
            onSwipeUp = { showAppDrawer() },
            onLongPress = { showCustomization() }
        )
        val content = home.content
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        header.gravity = Gravity.START
        header.addView(label(SimpleDateFormat("EEEE  •  d MMMM", Locale.getDefault()).format(Date()), 14f, Color.WHITE, true).apply {
            shadow(5f)
        })
        header.addView(label(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()), 66f, Color.WHITE, true).apply {
            setPadding(0, dp(1), 0, 0)
            shadow(8f)
            letterSpacing = -0.035f
        })
        header.addView(label(greetingText(), 15f, Color.WHITE, true).apply {
            shadow(5f)
            setPadding(0, dp(1), 0, 0)
        })
        content.addView(header, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })

        val widgets = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        widgetContainer = widgets
        val ids = widgetIds()
        ids.forEach { id ->
            try {
                val info = appWidgetManager.getAppWidgetInfo(id)
                if (info != null) {
                    val hostView = appWidgetHost.createView(this, id, info)
                    hostView.setAppWidget(id, info)
                    widgets.addView(hostView, LinearLayout.LayoutParams(-1, dp(150)).apply {
                        bottomMargin = dp(10)
                    })
                    hostView.setOnLongClickListener { showWidgetActions(id); true }
                }
            } catch (_: Exception) {}
        }
        if (ids.isNotEmpty()) {
            content.addView(widgets, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        }

        content.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f).apply { height = dp(20) })
        val favorites = selectedApps(HOME_KEY)
        val grid = GridView(this).apply {
            numColumns = 4
            horizontalSpacing = dp(8)
            verticalSpacing = dp(13)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
            adapter = AppGridAdapter(favorites)
            layoutParams = LinearLayout.LayoutParams(-1, dp(if (favorites.size > 4) 210 else 112))
            selector = ColorDrawable(Color.TRANSPARENT)
        }
        content.addView(grid)
        val search = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.argb(215, 255, 255, 255), dp(28).toFloat())
            setPadding(dp(18), 0, dp(18), 0)
            isClickable = true
            elevation = dp(3).toFloat()
            addView(label("⌕", 25f, Color.DKGRAY, true))
            addView(label("Search apps", 15f, Color.DKGRAY).apply { setPadding(dp(12), dp(15), 0, dp(15)) })
            setOnClickListener { showAppDrawer() }
        }
        content.addView(search, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.argb(178, 245, 247, 252), dp(30).toFloat())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            elevation = dp(5).toFloat()
        }
        selectedApps(DOCK_KEY).take(5).forEach { app ->
            val slot = FrameLayout(this).apply {
                isClickable = true
                setOnClickListener { launch(app.info) }
                setOnLongClickListener { showCustomization(); true }
            }
            slot.addView(ImageView(this).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
            }, FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER))
            dock.addView(slot, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
        content.addView(dock, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(14) })
        val hint = label("Swipe up for all apps  •  Hold an empty area to customize", 10f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            shadow()
            setPadding(0, dp(8), 0, 0)
        }
        content.addView(hint)
        setContentView(home)
    }

    private fun TextView.shadow(radius: Float = 4f): TextView {
        setShadowLayer(radius, 0f, 2f, Color.BLACK)
        return this
    }

    private inner class AppGridAdapter(initialItems: List<LauncherApp>) : BaseAdapter() {
        private val items = initialItems.toMutableList()
        fun submit(newItems: List<LauncherApp>) { items.clear(); items.addAll(newItems); notifyDataSetChanged() }
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
                icon = views.first; name = views.second
            } else {
                tile = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    setPadding(dp(3), dp(7), dp(3), dp(7)); minimumHeight = dp(96)
                    isClickable = true; isFocusable = true
                }
                icon = ImageView(this@MainActivity).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                tile.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
                name = label("", 11f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; shadow()
                }
                tile.addView(name, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
                tile.tag = Pair(icon, name)
            }
            val app = getItem(position)
            icon.setImageDrawable(app.icon); name.text = app.title
            tile.setOnClickListener { launch(app.info) }
            tile.setOnLongClickListener { showCustomization(); true }
            return tile
        }
    }

    private fun launch(info: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
        try { startActivity(intent) } catch (_: Exception) {}
    }

    private fun showCustomization() {
        val options = arrayOf("Choose home screen apps", "Customize dock apps", "Add a widget", "Manage widgets")
        AlertDialog.Builder(this)
            .setTitle("Home screen settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> chooseApps(HOME_KEY, "Home screen apps", 8)
                    1 -> chooseApps(DOCK_KEY, "Dock apps", 5)
                    2 -> pickWidget()
                    3 -> manageWidgets()
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun chooseApps(key: String, title: String, max: Int) {
        val chosen = selectedApps(key).toMutableList()
        val checked = BooleanArray(apps.size) { i -> chosen.any { it.key == apps[i].key } }
        AlertDialog.Builder(this)
            .setTitle("$title (up to $max)")
            .setMultiChoiceItems(apps.map { it.title }.toTypedArray(), checked) { dialog, which, isChecked ->
                val app = apps[which]
                if (isChecked) {
                    if (chosen.size < max) chosen.add(app)
                    else {
                        (dialog as? AlertDialog)?.listView?.setItemChecked(which, false)
                        android.widget.Toast.makeText(this, "Maximum $max apps", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else chosen.removeAll { it.key == app.key }
            }
            .setPositiveButton("Save") { _, _ -> saveSelection(key, chosen) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun pickWidget() {
        val id = appWidgetHost.allocateAppWidgetId()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        try { startActivityForResult(intent, REQUEST_PICK_WIDGET) }
        catch (_: Exception) {
            appWidgetHost.deleteAppWidgetId(id)
            android.widget.Toast.makeText(this, "Widget picker is unavailable", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun showWidgetActions(id: Int) {
        AlertDialog.Builder(this).setTitle("Widget")
            .setItems(arrayOf("Remove widget")) { _, _ ->
                appWidgetHost.deleteAppWidgetId(id)
                launcherPrefs.saveWidgetIds(widgetIds().filter { it != id })
                showHome()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun manageWidgets() {
        val ids = widgetIds()
        if (ids.isEmpty()) {
            AlertDialog.Builder(this).setMessage("No widgets added yet.")
                .setPositiveButton("Add widget") { _, _ -> pickWidget() }.setNegativeButton("Close", null).show()
            return
        }
        val names = ids.map { id -> appWidgetManager.getAppWidgetInfo(id)?.loadLabel(packageManager)?.toString() ?: "Widget $id" }
        AlertDialog.Builder(this).setTitle("Tap a widget to remove")
            .setItems(names.toTypedArray()) { _, which ->
                val id = ids[which]
                appWidgetHost.deleteAppWidgetId(id)
                launcherPrefs.saveWidgetIds(widgetIds().filter { it != id })
                showHome()
            }.setNegativeButton("Close", null).show()
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
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heading.addView(label("All apps", 27f, Color.WHITE, true).shadow(), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(label(apps.size.toString() + " apps", 12f, Color.WHITE).shadow())
        dialogContent.addView(heading)
        val search = EditText(this).apply {
            hint = "Search apps"; isSingleLine = true; textSize = 15f
            setTextColor(ink); setHintTextColor(muted)
            background = rounded(Color.argb(225, 255, 255, 255), dp(22).toFloat())
            setPadding(dp(17), 0, dp(17), 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        }
        dialogContent.addView(search, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(18); bottomMargin = dp(12) })
        val grid = GridView(this).apply {
            numColumns = 4; horizontalSpacing = dp(8); verticalSpacing = dp(10)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH; isVerticalScrollBarEnabled = false
            selector = ColorDrawable(Color.TRANSPARENT)
        }
        drawerAdapter = AppGridAdapter(apps); grid.adapter = drawerAdapter; drawerSearch = search
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

    companion object {
        private const val HOME_KEY = "home_apps"
        private const val DOCK_KEY = "dock_apps"
        private const val WIDGETS_KEY = "widget_ids"
        private const val REQUEST_PICK_WIDGET = 4101
        private const val REQUEST_CONFIGURE_WIDGET = 4102
    }
}
