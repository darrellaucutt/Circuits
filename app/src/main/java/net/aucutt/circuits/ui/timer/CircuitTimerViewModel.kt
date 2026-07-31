package net.aucutt.circuits.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.aucutt.circuits.data.CircuitEntity
import net.aucutt.circuits.data.CircuitsDatabase
import net.aucutt.circuits.service.CircuitTimerService
import net.aucutt.circuits.timer.CircuitTimerEngine

class CircuitTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val circuitDao = CircuitsDatabase.getInstance(application).circuitDao()

    val uiState = CircuitTimerEngine.uiState

    val savedCircuits: StateFlow<List<CircuitEntity>> = circuitDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private var baselineConfig: TimerConfig = uiState.value.config
    private var loadedCircuitId: Long? = null

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _loadedName = MutableStateFlow("")
    val loadedName: StateFlow<String> = _loadedName.asStateFlow()

    fun updateInterval(minutes: Int) {
        CircuitTimerEngine.updateInterval(minutes)
        refreshDirty()
    }

    fun updateCooldown(minutes: Int) {
        CircuitTimerEngine.updateCooldown(minutes)
        refreshDirty()
    }

    fun updateRepeats(count: Int) {
        CircuitTimerEngine.updateRepeats(count)
        refreshDirty()
    }

    fun saveCircuit(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val config = uiState.value.config
            val existingId = loadedCircuitId
            if (existingId != null) {
                circuitDao.update(CircuitEntity.from(trimmed, config, existingId))
            } else {
                loadedCircuitId = circuitDao.insert(CircuitEntity.from(trimmed, config))
            }
            markClean(config, trimmed)
        }
    }

    fun loadCircuit(circuit: CircuitEntity) {
        CircuitTimerEngine.applyConfig(circuit.toConfig())
        loadedCircuitId = circuit.id
        markClean(circuit.toConfig(), circuit.name)
    }

    fun deleteAllCircuits() {
        viewModelScope.launch {
            circuitDao.deleteAll()
            loadedCircuitId = null
            _loadedName.value = ""
            refreshDirty()
        }
    }

    fun start() = CircuitTimerService.start(getApplication())

    fun pause() = CircuitTimerService.pause(getApplication())

    fun resume() = CircuitTimerService.resume(getApplication())

    fun stop() = CircuitTimerService.stop(getApplication())

    fun resetToSetup() {
        CircuitTimerEngine.resetToSetup()
        runCatching { CircuitTimerService.reset(getApplication()) }
    }

    private fun refreshDirty() {
        _isDirty.value = uiState.value.config != baselineConfig
    }

    private fun markClean(config: TimerConfig, name: String) {
        baselineConfig = config
        _loadedName.value = name
        _isDirty.value = false
    }
}
