package com.knowledgebase.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knowledgebase.app.ui.viewmodel.OpenTabPersistence

/**
 * Stores app-level preferences: the selected KB folder URI, theme mode,
 * the last open note, and the set of open tabs (JSON).
 */
class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kb_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getKbUri(): String? = prefs.getString(KEY_KB_URI, null)

    fun getKbName(): String? = prefs.getString(KEY_KB_NAME, null)

    fun setKnowledgeBase(uri: String, name: String) {
        prefs.edit()
            .putString(KEY_KB_URI, uri)
            .putString(KEY_KB_NAME, name)
            .apply()
    }

    fun clearKnowledgeBase() {
        prefs.edit()
            .remove(KEY_KB_URI)
            .remove(KEY_KB_NAME)
            .apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }

    fun getLastOpenNote(): String? = prefs.getString(KEY_LAST_NOTE, null)

    fun setLastOpenNote(path: String?) {
        prefs.edit().apply {
            if (path == null) remove(KEY_LAST_NOTE) else putString(KEY_LAST_NOTE, path)
        }.apply()
    }

    fun getOpenTabs(): List<OpenTabPersistence> {
        val json = prefs.getString(KEY_OPEN_TABS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<OpenTabPersistence>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOpenTabs(tabs: List<OpenTabPersistence>) {
        prefs.edit().putString(KEY_OPEN_TABS, gson.toJson(tabs)).apply()
    }

    companion object {
        private const val KEY_KB_URI = "kb_uri"
        private const val KEY_KB_NAME = "kb_name"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LAST_NOTE = "last_open_note"
        private const val KEY_OPEN_TABS = "open_tabs"
    }
}