package net.aucutt.circuits.ui.distance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.aucutt.circuits.distance.DistanceCircuitEngine
import net.aucutt.circuits.service.DistanceCircuitService

class DistanceCircuitViewModel(application: Application) : AndroidViewModel(application) {

    val uiState = DistanceCircuitEngine.uiState

    fun updateHalfMiles(halfMiles: Int) {
        DistanceCircuitEngine.updateHalfMiles(halfMiles)
    }

    fun updateCooldown(minutes: Int) {
        DistanceCircuitEngine.updateCooldown(minutes)
    }

    fun updateRepeats(count: Int) {
        DistanceCircuitEngine.updateRepeats(count)
    }

    fun start() = DistanceCircuitService.start(getApplication())

    fun pause() = DistanceCircuitService.pause(getApplication())

    fun resume() = DistanceCircuitService.resume(getApplication())

    fun stop() = DistanceCircuitService.stop(getApplication())

    fun resetToSetup() {
        DistanceCircuitEngine.resetToSetup()
        runCatching { DistanceCircuitService.reset(getApplication()) }
    }
}
