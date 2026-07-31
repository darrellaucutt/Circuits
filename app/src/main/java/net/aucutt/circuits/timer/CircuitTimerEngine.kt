package net.aucutt.circuits.timer

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
import net.aucutt.circuits.ui.timer.TimerConfig
import net.aucutt.circuits.ui.timer.TimerPhase
import net.aucutt.circuits.ui.timer.TimerUiState

/**
 * Process-scoped timer engine so the countdown survives Activity backgrounding
 * when paired with [net.aucutt.circuits.service.CircuitTimerService].
 */
object CircuitTimerEngine {

    private const val PRE_WORKOUT_SECONDS = 30

    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerScope: CoroutineScope = defaultScope
    private var tickerJob: Job? = null

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _announcements = MutableSharedFlow<TimerAnnouncement>(extraBufferCapacity = 8)
    val announcements: SharedFlow<TimerAnnouncement> = _announcements.asSharedFlow()

    fun updateInterval(minutes: Int) {
        if (_uiState.value.phase != TimerPhase.Idle) return
        _uiState.update { it.copy(config = it.config.withInterval(minutes)) }
    }

    fun updateCooldown(minutes: Int) {
        if (_uiState.value.phase != TimerPhase.Idle) return
        _uiState.update { it.copy(config = it.config.withCooldown(minutes)) }
    }

    fun updateRepeats(count: Int) {
        if (_uiState.value.phase != TimerPhase.Idle) return
        _uiState.update { it.copy(config = it.config.withRepeats(count)) }
    }

    fun applyConfig(config: TimerConfig) {
        if (_uiState.value.phase != TimerPhase.Idle) return
        _uiState.update {
            it.copy(
                config = TimerConfig(
                    intervalMinutes = config.intervalMinutes.coerceAtLeast(1),
                    cooldownMinutes = config.cooldownMinutes.coerceAtLeast(0),
                    repeats = config.repeats.coerceAtLeast(1),
                ),
            )
        }
    }

    fun start() {
        val state = _uiState.value
        if (state.phase != TimerPhase.Idle && state.phase != TimerPhase.Finished) return

        val config = state.config
        _uiState.value = TimerUiState(
            config = config,
            phase = TimerPhase.PreWorkout,
            remainingSeconds = PRE_WORKOUT_SECONDS,
            currentRound = 1,
            isPaused = false,
        )
        _announcements.tryEmit(TimerAnnouncement.PreWorkout)
        startTicker()
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
        startTicker()
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update { TimerUiState(config = it.config) }
    }

    fun resetToSetup() {
        stop()
    }

    /** Restores idle state and cancels the ticker. For unit tests only. */
    internal fun resetForTests() {
        tickerJob?.cancel()
        tickerJob = null
        _uiState.value = TimerUiState()
    }

    /** Overrides the coroutine scope used by the ticker. For unit tests only. */
    internal fun setTickerScopeForTests(scope: CoroutineScope) {
        tickerScope = scope
    }

    /** Restores the production ticker scope. For unit tests only. */
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

    private fun advancePhase(state: TimerUiState) {
        val config = state.config
        when (state.phase) {
            TimerPhase.PreWorkout -> {
                _uiState.update {
                    it.copy(
                        phase = TimerPhase.Work,
                        remainingSeconds = config.intervalSeconds,
                        currentRound = 1,
                    )
                }
                announceWork(1)
            }

            TimerPhase.Work -> {
                if (config.cooldownSeconds > 0) {
                    _uiState.update {
                        it.copy(
                            phase = TimerPhase.Cooldown,
                            remainingSeconds = config.cooldownSeconds,
                        )
                    }
                    _announcements.tryEmit(TimerAnnouncement.Cooldown)
                } else {
                    advanceAfterCooldown(state)
                }
            }

            TimerPhase.Cooldown -> advanceAfterCooldown(state)

            TimerPhase.Idle, TimerPhase.Finished -> Unit
        }
    }

    private fun advanceAfterCooldown(state: TimerUiState) {
        val config = state.config
        if (state.currentRound < config.repeats) {
            val nextRound = state.currentRound + 1
            _uiState.update {
                it.copy(
                    phase = TimerPhase.Work,
                    remainingSeconds = config.intervalSeconds,
                    currentRound = nextRound,
                )
            }
            announceWork(nextRound)
        } else {
            tickerJob?.cancel()
            tickerJob = null
            _uiState.update {
                it.copy(
                    phase = TimerPhase.Finished,
                    remainingSeconds = 0,
                    isPaused = false,
                )
            }
            _announcements.tryEmit(TimerAnnouncement.Complete)
        }
    }

    private fun announceWork(round: Int) {
        _announcements.tryEmit(TimerAnnouncement.Work(round))
    }
}
