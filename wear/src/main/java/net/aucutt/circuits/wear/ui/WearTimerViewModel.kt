package net.aucutt.circuits.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.aucutt.circuits.sync.SyncConfig
import net.aucutt.circuits.timer.CircuitTimerEngine
import net.aucutt.circuits.wear.service.WearTimerService
import net.aucutt.circuits.wear.sync.WearSyncRepository

class WearTimerViewModel(application: Application) : AndroidViewModel(application) {

    val uiState = CircuitTimerEngine.uiState
    val circuits = WearSyncRepository.circuits
    val activeConfig = WearSyncRepository.activeConfig
    val selectedCircuitId = WearSyncRepository.selectedCircuitId

    fun selectCircuit(circuitId: Long) {
        WearSyncRepository.selectCircuit(circuitId)
    }

    fun start() {
        WearSyncRepository.applySelectedConfigToEngine()
        WearTimerService.start(getApplication())
    }

    fun pause() = WearTimerService.pause(getApplication())

    fun resume() = WearTimerService.resume(getApplication())

    fun stop() = WearTimerService.stop(getApplication())

    fun resetToSetup() {
        CircuitTimerEngine.resetToSetup()
        runCatching { WearTimerService.reset(getApplication()) }
    }

    fun configSummary(config: SyncConfig): String {
        return "${config.intervalMinutes}/${config.cooldownMinutes} · ${config.repeats} rounds"
    }
}
