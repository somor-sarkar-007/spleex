package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.spleexDataStore by preferencesDataStore(name = "spleex_session")

class SessionPreferences(private val context: Context) {

    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }

    val currentUserId: Flow<String?> = context.spleexDataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USER_ID]?.takeIf { it.isNotBlank() }
    }

    val isDarkMode: Flow<Boolean> = context.spleexDataStore.data.map { prefs ->
        prefs[Keys.IS_DARK_MODE] ?: true
    }

    val hasSeenOnboarding: Flow<Boolean> = context.spleexDataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_ONBOARDING] ?: false
    }

    val recentSearches: Flow<List<String>> = context.spleexDataStore.data.map { prefs ->
        prefs[Keys.RECENT_SEARCHES]
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.spleexDataStore.edit { prefs ->
            if (userId.isNullOrBlank()) {
                prefs.remove(Keys.CURRENT_USER_ID)
            } else {
                prefs[Keys.CURRENT_USER_ID] = userId
            }
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.spleexDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setOnboardingSeen() {
        context.spleexDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_ONBOARDING] = true
        }
    }

    suspend fun addRecentSearch(query: String) {
        val cleaned = query.trim().replace("|", "")
        if (cleaned.isBlank()) return
        context.spleexDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() && !it.equals(cleaned, ignoreCase = true) }
                ?: emptyList()
            val updated = (listOf(cleaned) + current).take(8)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString("|")
        }
    }

    suspend fun clearRecentSearches() {
        context.spleexDataStore.edit { prefs ->
            prefs.remove(Keys.RECENT_SEARCHES)
        }
    }
}
