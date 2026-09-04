package com.manglamfertilizer.app.data.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Collections

/**
 * Lightweight tracker to prevent resurrection of deleted records.
 * Keeps an in-memory set and persists to SharedPreferences across application restarts.
 * Ensures that background Firestore snapshot listeners or cached local items never
 * re-insert deleted records into Room or the UI.
 */
object DeletedRecordsTracker {
  private val inMemoryDeletedIds = Collections.synchronizedSet(mutableSetOf<String>())
  private var sharedPreferences: SharedPreferences? = null

  fun init(context: Context) {
    if (sharedPreferences == null) {
      try {
        val prefs = context.applicationContext.getSharedPreferences("manglam_deleted_records", Context.MODE_PRIVATE)
        sharedPreferences = prefs
        val stored = prefs.getStringSet("deleted_ids", emptySet()) ?: emptySet()
        inMemoryDeletedIds.addAll(stored)
      } catch (_: Exception) {}
    }
  }

  fun markDeleted(id: String) {
    if (id.isBlank()) return
    inMemoryDeletedIds.add(id)
    try {
      sharedPreferences?.let { prefs ->
        val current = prefs.getStringSet("deleted_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(id)
        if (current.size > 1000) {
          val trimmed = current.toList().takeLast(800).toSet()
          prefs.edit().putStringSet("deleted_ids", trimmed).apply()
        } else {
          prefs.edit().putStringSet("deleted_ids", current).apply()
        }
      }
    } catch (_: Exception) {}
  }

  fun isDeleted(id: String?): Boolean {
    if (id.isNullOrBlank()) return false
    return inMemoryDeletedIds.contains(id)
  }

  fun clearForTest() {
    inMemoryDeletedIds.clear()
    try {
      sharedPreferences?.edit()?.clear()?.apply()
    } catch (_: Exception) {}
  }
}
