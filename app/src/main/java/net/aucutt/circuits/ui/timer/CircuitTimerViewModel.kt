package net.aucutt.circuits.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.aucutt.circuits.service.CircuitTimerService
import net.aucutt.circuits.timer.CircuitTimerEngine

class CircuitTimerViewModel(application: Application) : AndroidViewModel(application) {

    val uiState = CircuitTimerEngine.uiState

    fun updateInterval(minutes: Int) = CircuitTimerEngine.updateInterval(minutes)

    fun updateCooldown(minutes: Int) = CircuitTimerEngine.updateCooldown(minutes)

    fun updateRepeats(count: Int) = CircuitTimerEngine.updateRepeats(count)

    fun start() = CircuitTimerService.start(getApplication())

    fun pause() = CircuitTimerService.pause(getApplication())

    fun resume() = CircuitTimerService.resume(getApplication())

    fun stop() = CircuitTimerService.stop(getApplication())

    fun resetToSetup() {
        CircuitTimerEngine.resetToSetup()
        // Best-effort: stop an in-flight service (e.g. during the post-complete window).
        runCatching { CircuitTimerService.reset(getApplication()) }
    }
}
