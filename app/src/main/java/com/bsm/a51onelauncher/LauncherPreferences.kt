package com.bsm.a51onelauncher

import android.content.Context

/**
 * Owns persistence for launcher selections and hosted widget IDs.
 * UI components should use this class instead of accessing SharedPreferences directly.
 */
class LauncherPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("one_ui_home", Context.MODE_PRIVATE)

    fun initializeDefaults(homeAppKeys: List<String>, dockAppKeys: List<String>) {
        if (!preferences.contains(HOME_APPS_KEY)) {
            saveSelectedAppKeys(HOME_APPS_KEY, homeAppKeys)
        }
        if (!preferences.contains(DOCK_APPS_KEY)) {
            saveSelectedAppKeys(DOCK_APPS_KEY, dockAppKeys)
        }
    }

    fun selectedAppKeys(key: String): List<String> =
        preferences.getString(key, "").orEmpty()
            .split("|")
            .filter { it.isNotBlank() }

    fun saveSelectedAppKeys(key: String, appKeys: List<String>) {
        preferences.edit().putString(key, appKeys.joinToString("|")).apply()
    }

    fun widgetIds(): List<Int> =
        preferences.getString(WIDGET_IDS_KEY, "").orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .filter { it > 0 }

    fun saveWidgetIds(ids: List<Int>) {
        preferences.edit().putString(WIDGET_IDS_KEY, ids.joinToString(",")).apply()
    }

    companion object {
        const val HOME_APPS_KEY = "home_apps"
        const val DOCK_APPS_KEY = "dock_apps"
        private const val WIDGET_IDS_KEY = "widget_ids"
    }
}
