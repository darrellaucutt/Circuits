package net.aucutt.circuits.wear.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.aucutt.circuits.sync.SyncCircuit
import net.aucutt.circuits.sync.SyncConfig
import net.aucutt.circuits.sync.WearCommand
import net.aucutt.circuits.sync.WearCommandAction
import net.aucutt.circuits.timer.CircuitTimerEngine

object WearSyncRepository {

    private val _circuits = MutableStateFlow<List<SyncCircuit>>(emptyList())
    val circuits: StateFlow<List<SyncCircuit>> = _circuits.asStateFlow()

    private val _activeConfig = MutableStateFlow<SyncConfig?>(null)
    val activeConfig: StateFlow<SyncConfig?> = _activeConfig.asStateFlow()

    private val _selectedCircuitId = MutableStateFlow<Long?>(null)
    val selectedCircuitId: StateFlow<Long?> = _selectedCircuitId.asStateFlow()

    private var lastCommandTimestamp = 0L

    fun updateCircuits(circuits: List<SyncCircuit>) {
        _circuits.value = circuits
        if (_selectedCircuitId.value != null && circuits.none { it.id == _selectedCircuitId.value }) {
            _selectedCircuitId.value = circuits.firstOrNull()?.id
        } else if (_selectedCircuitId.value == null) {
            _selectedCircuitId.value = circuits.firstOrNull()?.id
        }
    }

    fun updateConfig(config: SyncConfig) {
        _activeConfig.value = config
        applyConfigToEngine(config)
    }

    fun selectCircuit(circuitId: Long) {
        _selectedCircuitId.value = circuitId
        _circuits.value.firstOrNull { it.id == circuitId }?.let { circuit ->
            val config = SyncConfig(
                name = circuit.name,
                intervalMinutes = circuit.intervalMinutes,
                cooldownMinutes = circuit.cooldownMinutes,
                repeats = circuit.repeats,
            )
            _activeConfig.value = config
            applyConfigToEngine(config)
        }
    }

    fun handleCommand(command: WearCommand) {
        if (command.timestamp <= lastCommandTimestamp) return
        lastCommandTimestamp = command.timestamp

        when (command.action) {
            WearCommandAction.START -> {
                activeConfig.value?.let { applyConfigToEngine(it) }
                WearTimerServiceBridge.start()
            }
            WearCommandAction.PAUSE -> WearTimerServiceBridge.pause()
            WearCommandAction.RESUME -> WearTimerServiceBridge.resume()
            WearCommandAction.STOP -> WearTimerServiceBridge.stop()
        }
    }

    fun selectedConfig(): SyncConfig? {
        val selectedId = _selectedCircuitId.value
        if (selectedId != null) {
            _circuits.value.firstOrNull { it.id == selectedId }?.let { circuit ->
                return SyncConfig(
                    name = circuit.name,
                    intervalMinutes = circuit.intervalMinutes,
                    cooldownMinutes = circuit.cooldownMinutes,
                    repeats = circuit.repeats,
                )
            }
        }
        return _activeConfig.value
    }

    fun applySelectedConfigToEngine() {
        selectedConfig()?.let { applyConfigToEngine(it) }
    }

    private fun applyConfigToEngine(config: SyncConfig) {
        if (CircuitTimerEngine.uiState.value.phase == net.aucutt.circuits.model.TimerPhase.Idle ||
            CircuitTimerEngine.uiState.value.phase == net.aucutt.circuits.model.TimerPhase.Finished
        ) {
            CircuitTimerEngine.applyConfig(config.toConfig())
        }
    }
}

interface WearTimerServiceBridge {
    fun start()
    fun pause()
    fun resume()
    fun stop()

    companion object : WearTimerServiceBridge {
        var delegate: WearTimerServiceBridge = NoOpWearTimerServiceBridge

        override fun start() = delegate.start()
        override fun pause() = delegate.pause()
        override fun resume() = delegate.resume()
        override fun stop() = delegate.stop()
    }
}

private object NoOpWearTimerServiceBridge : WearTimerServiceBridge {
    override fun start() = Unit
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() = Unit
}
