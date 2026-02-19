package com.example.ghostespcompanion.data.repository

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ghostespcompanion.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for applying app settings functionality
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "ghostesp_status"
        
        const val HAPTIC_LIGHT = 1
        const val HAPTIC_MEDIUM = 2
        const val HAPTIC_HEAVY = 3
        const val HAPTIC_SUCCESS = 4
        const val HAPTIC_ERROR = 5
    }

    /**
     * Perform haptic feedback if enabled
     */
    fun performHapticFeedback(enabled: Boolean, type: Int = HAPTIC_LIGHT) {
        if (!enabled || !vibrator.hasVibrator()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                HAPTIC_LIGHT -> VibrationEffect.createOneShot(10, 50)
                HAPTIC_MEDIUM -> VibrationEffect.createOneShot(15, 100)
                HAPTIC_HEAVY -> VibrationEffect.createOneShot(25, 200)
                HAPTIC_SUCCESS -> VibrationEffect.createWaveform(
                    longArrayOf(0, 10, 50, 10), 
                    intArrayOf(0, 100, 0, 50), 
                    -1
                )
                HAPTIC_ERROR -> VibrationEffect.createWaveform(
                    longArrayOf(0, 20, 30, 20, 30, 20), 
                    intArrayOf(0, 100, 0, 100, 0, 100), 
                    -1
                )
                else -> VibrationEffect.createOneShot(10, 50)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(when (type) {
                HAPTIC_LIGHT -> 10L
                HAPTIC_MEDIUM -> 15L
                HAPTIC_HEAVY -> 25L
                else -> 10L
            })
        }
    }

    /**
     * Show a status notification if enabled
     */
    fun showNotification(title: String, message: String, enabled: Boolean) {
        if (!enabled) return
        
        try {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted, ignore
        }
    }

    /**
     * Check if auto-connect is enabled and get last device address
     */
    suspend fun shouldAutoConnect(): Boolean {
        return preferencesRepository.appSettings.first().autoConnect
    }
}
