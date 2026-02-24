package com.example.ghostespcompanion.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PhoneLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speed: Float?,
    val accuracy: Float?,
    val bearing: Float?,
    val timestamp: Long
)

@Singleton
class LocationHelper @Inject constructor(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L
    ).apply {
        setMinUpdateIntervalMillis(2000L)
        setWaitForAccurateLocation(true)
    }.build()

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getLocationUpdates(): Flow<PhoneLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location.toPhoneLocation())
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    suspend fun getLastKnownLocation(): PhoneLocation? {
        if (!hasLocationPermission()) return null

        return try {
            val location = fusedLocationClient.lastLocation.await()
            location?.toPhoneLocation()
        } catch (e: SecurityException) {
            null
        }
    }

    private fun Location.toPhoneLocation() = PhoneLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = if (hasAltitude()) altitude else null,
        speed = if (hasSpeed()) speed else null,
        accuracy = if (hasAccuracy()) accuracy else null,
        bearing = if (hasBearing()) bearing else null,
        timestamp = time
    )
}
