package com.example.ghostespcompanion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for GhostESP Companion
 * 
 * This is the entry point for the Hilt dependency injection framework.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                "ghostesp_downloads",
                "Downloads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "File download notifications"
            }

            val statusChannel = NotificationChannel(
                "ghostesp_status",
                "Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Device status notifications"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(downloadChannel, statusChannel))
        }
    }
}
