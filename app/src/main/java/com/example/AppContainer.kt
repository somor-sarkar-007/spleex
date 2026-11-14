package com.example

import android.content.Context
import com.example.data.local.SpleexDatabase
import com.example.data.local.SpleexSeedInitializer
import com.example.data.media.MediaStorageManager
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.SpleexRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: SpleexDatabase by lazy { SpleexDatabase.getInstance(appContext) }
    val mediaStorage: MediaStorageManager by lazy { MediaStorageManager(appContext) }
    val sessionPrefs: SessionPreferences by lazy { SessionPreferences(appContext) }
    val repository: SpleexRepository by lazy {
        SpleexRepository(
            dao = database.spleexDao(),
            mediaStorage = mediaStorage,
            sessionPrefs = sessionPrefs
        )
    }

    suspend fun initializeSeedData() {
        SpleexSeedInitializer.ensureSeeded(database, mediaStorage)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context).also { INSTANCE = it }
            }
        }
    }
}
