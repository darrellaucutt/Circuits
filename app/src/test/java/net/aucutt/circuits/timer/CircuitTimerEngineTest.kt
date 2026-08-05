package net.aucutt.circuits.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.aucutt.circuits.ui.timer.TimerConfig
import net.aucutt.circuits.ui.timer.TimerPhase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CircuitTimerEngineTest {

    private lateinit var scheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        CircuitTimerEngine.resetForTests()
        scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        CircuitTimerEngine.setTickerScopeForTests(CoroutineScope(SupervisorJob() + dispatcher))
    }

    @After
    fun tearDown() {
        CircuitTimerEngine.resetForTests()
        CircuitTimerEngine.restoreDefaultTickerScopeForTests()
    }

    private fun advanceTime(millis: Long) {
        // +1 ms so delays scheduled exactly at the boundary still fire.
        scheduler.advanceTimeBy(millis + 1)
    }

    @Test
    fun start_fromIdle_entersPreWorkoutAt30Seconds() {
        CircuitTimerEngine.start()

        val state = CircuitTimerEngine.uiState.value
        assertEquals(TimerPhase.PreWorkout, state.phase)
        assertEquals(30, state.remainingSeconds)
        assertEquals(1, state.currentRound)
        assertFalse(state.isPaused)
        assertTrue(state.isRunning)
    }

    @Test
    fun start_emitsPreWorkoutAnnouncement() {
        val dispatcher = StandardTestDispatcher(scheduler)
        val announcements = mutableListOf<TimerAnnouncement>()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val job = scope.launch {
            CircuitTimerEngine.announcements.collect { announcements.add(it) }
        }
        scheduler.runCurrent()

        CircuitTimerEngine.start()
        scheduler.runCurrent()
        job.cancel()

        assertEquals(listOf(TimerAnnouncement.PreWorkout), announcements)
    }

    @Test
    fun afterPreWorkoutCountdown_emitsWorkoutStart() {
        val dispatcher = StandardTestDispatcher(scheduler)
        val announcements = mutableListOf<TimerAnnouncement>()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val job = scope.launch {
            CircuitTimerEngine.announcements.collect { announcements.add(it) }
        }
        scheduler.runCurrent()

        CircuitTimerEngine.start()
        advanceTime(30_000)
        scheduler.runCurrent()
        job.cancel()

        assertTrue(announcements.contains(TimerAnnouncement.PreWorkout))
        assertTrue(announcements.contains(TimerAnnouncement.WorkoutStart(1)))
    }

    @Test
    fun afterPreWorkoutCountdown_transitionsToWorkRound1() {
        CircuitTimerEngine.start()
        advanceTime(30_000)

        val state = CircuitTimerEngine.uiState.value
        assertEquals(TimerPhase.Work, state.phase)
        assertEquals(60, state.remainingSeconds)
        assertEquals(1, state.currentRound)
    }

    @Test
    fun afterWorkInterval_transitionsToCooldown() {
        CircuitTimerEngine.applyConfig(TimerConfig(intervalMinutes = 1, cooldownMinutes = 1, repeats = 1))

        CircuitTimerEngine.start()
        advanceTime(30_000 + 60_000)

        val state = CircuitTimerEngine.uiState.value
        assertEquals(TimerPhase.Cooldown, state.phase)
        assertEquals(60, state.remainingSeconds)
    }

    @Test
    fun afterFinalCooldown_completesCircuit() {
        val dispatcher = StandardTestDispatcher(scheduler)
        val announcements = mutableListOf<TimerAnnouncement>()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val job = scope.launch {
            CircuitTimerEngine.announcements.collect { announcements.add(it) }
        }
        scheduler.runCurrent()

        CircuitTimerEngine.applyConfig(TimerConfig(intervalMinutes = 1, cooldownMinutes = 1, repeats = 1))
        CircuitTimerEngine.start()
        advanceTime(30_000 + 60_000 + 60_000)
        scheduler.runCurrent()
        job.cancel()

        val state = CircuitTimerEngine.uiState.value
        assertEquals(TimerPhase.Finished, state.phase)
        assertEquals(0, state.remainingSeconds)
        assertFalse(state.isRunning)
        assertTrue(announcements.contains(TimerAnnouncement.Complete))
        assertTrue(announcements.contains(TimerAnnouncement.WorkoutStart(1)))
    }

    @Test
    fun workWithoutCooldown_skipsToNextRound() {
        CircuitTimerEngine.applyConfig(TimerConfig(intervalMinutes = 1, cooldownMinutes = 0, repeats = 2))
        CircuitTimerEngine.start()
        advanceTime(30_000 + 60_000)

        val state = CircuitTimerEngine.uiState.value
        assertEquals(TimerPhase.Work, state.phase)
        assertEquals(2, state.currentRound)
        assertEquals(60, state.remainingSeconds)
    }

    @Test
    fun pause_stopsCountdown() {
        CircuitTimerEngine.start()
        advanceTime(5_000)

        CircuitTimerEngine.pause()
        val pausedAt = CircuitTimerEngine.uiState.value.remainingSeconds
        advanceTime(10_000)

        assertEquals(pausedAt, CircuitTimerEngine.uiState.value.remainingSeconds)
        assertTrue(CircuitTimerEngine.uiState.value.isPaused)
    }

    @Test
    fun resume_continuesCountdown() {
        CircuitTimerEngine.start()
        advanceTime(5_000)
        CircuitTimerEngine.pause()
        advanceTime(10_000)

        CircuitTimerEngine.resume()
        advanceTime(1_000)

        assertEquals(24, CircuitTimerEngine.uiState.value.remainingSeconds)
        assertFalse(CircuitTimerEngine.uiState.value.isPaused)
    }

    @Test
    fun stop_resetsToIdle() {
        CircuitTimerEngine.applyConfig(TimerConfig(intervalMinutes = 2, cooldownMinutes = 1, repeats = 5))
        CircuitTimerEngine.start()
        advanceTime(5_000)

        CircuitTimerEngine.stop()

        val state = CircuitTimerEngine.uiState.value
        assertEquals(TimerPhase.Idle, state.phase)
        assertEquals(TimerConfig(intervalMinutes = 2, cooldownMinutes = 1, repeats = 5), state.config)
        assertFalse(state.isRunning)
    }

    @Test
    fun configUpdates_areIgnoredWhileRunning() {
        CircuitTimerEngine.applyConfig(TimerConfig(intervalMinutes = 1, cooldownMinutes = 1, repeats = 3))
        CircuitTimerEngine.start()
        advanceTime(1_000)

        CircuitTimerEngine.updateInterval(5)
        CircuitTimerEngine.updateCooldown(0)
        CircuitTimerEngine.updateRepeats(10)
        CircuitTimerEngine.applyConfig(TimerConfig(intervalMinutes = 9, cooldownMinutes = 9, repeats = 9))

        assertEquals(
            TimerConfig(intervalMinutes = 1, cooldownMinutes = 1, repeats = 3),
            CircuitTimerEngine.uiState.value.config,
        )
    }

    @Test
    fun applyConfig_clampsInvalidValues() {
        CircuitTimerEngine.applyConfig(TimerConfig(intervalMinutes = 0, cooldownMinutes = -3, repeats = 0))

        assertEquals(
            TimerConfig(intervalMinutes = 1, cooldownMinutes = 0, repeats = 1),
            CircuitTimerEngine.uiState.value.config,
        )
    }
}
