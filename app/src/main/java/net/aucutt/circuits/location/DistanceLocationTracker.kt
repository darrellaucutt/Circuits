package net.aucutt.circuits.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class DistanceLocationTracker(
    context: Context,
    private val onFirstFix: () -> Unit,
    private val onDistanceDelta: (Double) -> Unit,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastLocation: Location? = null
    private var hasReportedFix = false
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {
        stop(resetFix = false)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_METERS)
            .setWaitForAccurateLocation(true)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (!location.hasAccuracy() || location.accuracy > MAX_ACCURACY_METERS) return

                if (!hasReportedFix) {
                    hasReportedFix = true
                    onFirstFix()
                }

                val previous = lastLocation
                if (previous != null) {
                    val delta = previous.distanceTo(location).toDouble()
                    if (delta >= MIN_DELTA_METERS) {
                        onDistanceDelta(delta)
                    }
                }
                lastLocation = location
            }
        }
        callback = locationCallback
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun stop(resetFix: Boolean = true) {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
        lastLocation = null
        if (resetFix) {
            hasReportedFix = false
        }
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 2_000L
        private const val MIN_UPDATE_METERS = 3f
        private const val MIN_DELTA_METERS = 1.0
        private const val MAX_ACCURACY_METERS = 40f
    }
}
