package net.aucutt.circuits.distance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.aucutt.circuits.ui.distance.DistanceConfig
import net.aucutt.circuits.ui.distance.DistanceMiles
import net.aucutt.circuits.ui.distance.DistancePhase
import net.aucutt.circuits.ui.distance.DistanceUiState

object DistanceCircuitEngine {

    private const val PRE_WORKOUT_SECONDS = 30

    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerScope: CoroutineScope = defaultScope
    private var tickerJob: Job? = null
    private var nextHalfMileMarkerMeters = DistanceMiles.METERS_PER_HALF_MILE

    private val _uiState = MutableStateFlow(DistanceUiState())
    val uiState: StateFlow<DistanceUiState> = _uiState.asStateFlow()

    private var _announcements = MutableSharedFlow<DistanceAnnouncement>(extraBufferCapacity = 32)
    val announcements: SharedFlow<DistanceAnnouncement>
        get() = _announcements.asSharedFlow()

    fun updateHalfMiles(halfMiles: Int) {
        if (_uiState.value.phase != DistancePhase.Idle) return
        _uiState.update { it.copy(config = it.config.withHalfMiles(halfMiles)) }
    }

    fun updateCooldown(minutes: Int) {
        if (_uiState.value.phase != DistancePhase.Idle) return
        _uiState.update { it.copy(config = it.config.withCooldown(minutes)) }
    }

    fun updateRepeats(count: Int) {
        if (_uiState.value.phase != DistancePhase.Idle) return
        _uiState.update { it.copy(config = it.config.withRepeats(count)) }
    }

    fun applyConfig(config: DistanceConfig) {
        if (_uiState.value.phase != DistancePhase.Idle) return
        _uiState.update {
            it.copy(
                config = DistanceConfig(
                    halfMiles = config.halfMiles.coerceAtLeast(
                        net.aucutt.circuits.ui.distance.DistanceMiles.MIN_HALF_MILES,
                    ),
                    cooldownMinutes = config.cooldownMinutes.coerceAtLeast(0),
                    repeats = config.repeats.coerceAtLeast(1),
                ),
            )
        }
    }

    fun start() {
        val state = _uiState.value
        if (state.phase != DistancePhase.Idle && state.phase != DistancePhase.Finished) return

        val config = state.config
        _uiState.value = DistanceUiState(
            config = config,
            phase = DistancePhase.PreWorkout,
            remainingSeconds = PRE_WORKOUT_SECONDS,
            isPaused = false,
            hasGpsFix = false,
        )
        _announcements.tryEmit(DistanceAnnouncement.PreWorkout)
        startTicker()
    }

    fun onGpsFixAcquired() {
        _uiState.update { if (it.phase == DistancePhase.Work) it.copy(hasGpsFix = true) else it }
    }

    fun addDistance(meters: Double) {
        if (meters <= 0.0) return
        val state = _uiState.value
        if (state.phase != DistancePhase.Work || state.isPaused) return

        val updatedMeters = state.distanceMeters + meters
        val targetMeters = state.config.targetMeters
        val cappedMeters = minOf(updatedMeters, targetMeters)

        announceHalfMileMarkers(cappedMeters, targetMeters)

        if (updatedMeters >= targetMeters) {
            _uiState.update { it.copy(distanceMeters = targetMeters, hasGpsFix = true) }
            completeWorkInterval(state.config)
        } else {
            _uiState.update { it.copy(distanceMeters = updatedMeters, hasGpsFix = true) }
        }
    }

    private fun announceHalfMileMarkers(distanceMeters: Double, targetMeters: Double) {
        while (nextHalfMileMarkerMeters <= distanceMeters && nextHalfMileMarkerMeters <= targetMeters) {
            val halfMilesCompleted =
                (nextHalfMileMarkerMeters / DistanceMiles.METERS_PER_HALF_MILE).toInt()
            _announcements.tryEmit(DistanceAnnouncement.HalfMileMarker(halfMilesCompleted))
            nextHalfMileMarkerMeters += DistanceMiles.METERS_PER_HALF_MILE
        }
    }

    private fun resetHalfMileMarkers() {
        nextHalfMileMarkerMeters = DistanceMiles.METERS_PER_HALF_MILE
    }

    fun pause() {
        val state = _uiState.value
        if (!state.isRunning || state.isPaused) return
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resume() {
        val state = _uiState.value
        if (!state.isRunning || !state.isPaused) return
        _uiState.update { it.copy(isPaused = false) }
        if (state.phase == DistancePhase.PreWorkout || state.phase == DistancePhase.Cooldown) {
            startTicker()
        }
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        resetHalfMileMarkers()
        _uiState.update { DistanceUiState(config = it.config) }
    }

    fun resetToSetup() {
        stop()
    }

    internal fun resetForTests() {
        tickerJob?.cancel()
        tickerJob = null
        resetHalfMileMarkers()
        _uiState.value = DistanceUiState()
        _announcements = MutableSharedFlow(extraBufferCapacity = 32)
    }

    internal fun setTickerScopeForTests(scope: CoroutineScope) {
        tickerScope = scope
    }

    internal fun restoreDefaultTickerScopeForTests() {
        tickerScope = defaultScope
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = tickerScope.launch {
            while (isActive) {
                delay(1_000)
                tick()
            }
        }
    }

    private fun tick() {
        val state = _uiState.value
        if (!state.isRunning || state.isPaused) return

        if (state.remainingSeconds > 1) {
            _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            return
        }

        advancePhase(state)
    }

    private fun advancePhase(state: DistanceUiState) {
        when (state.phase) {
            DistancePhase.PreWorkout -> {
                resetHalfMileMarkers()
                _uiState.update {
                    it.copy(
                        phase = DistancePhase.Work,
                        distanceMeters = 0.0,
                        remainingSeconds = 0,
                        currentRound = 1,
                        hasGpsFix = false,
                    )
                }
                _announcements.tryEmit(DistanceAnnouncement.WorkStart(1))
            }

            DistancePhase.Cooldown -> advanceAfterCooldown(state)

            DistancePhase.Idle, DistancePhase.Work, DistancePhase.Finished -> Unit
        }
    }

    private fun advanceAfterCooldown(state: DistanceUiState) {
        val config = state.config
        if (state.currentRound < config.repeats) {
            val nextRound = state.currentRound + 1
            resetHalfMileMarkers()
            _uiState.update {
                it.copy(
                    phase = DistancePhase.Work,
                    distanceMeters = 0.0,
                    remainingSeconds = 0,
                    currentRound = nextRound,
                    isPaused = false,
                    hasGpsFix = false,
                )
            }
            _announcements.tryEmit(DistanceAnnouncement.WorkStart(nextRound))
        } else {
            finishCircuit()
        }
    }

    private fun completeWorkInterval(config: DistanceConfig) {
        tickerJob?.cancel()
        tickerJob = null
        _announcements.tryEmit(DistanceAnnouncement.WorkComplete)
        if (config.cooldownSeconds > 0) {
            _uiState.update {
                it.copy(
                    phase = DistancePhase.Cooldown,
                    distanceMeters = config.targetMeters,
                    remainingSeconds = config.cooldownSeconds,
                    isPaused = false,
                    hasGpsFix = false,
                )
            }
            _announcements.tryEmit(DistanceAnnouncement.Cooldown)
            startTicker()
        } else {
            advanceAfterCooldown(_uiState.value)
        }
    }

    private fun finishCircuit() {
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update {
            it.copy(
                phase = DistancePhase.Finished,
                remainingSeconds = 0,
                isPaused = false,
            )
        }
        _announcements.tryEmit(DistanceAnnouncement.Complete)
    }
}
