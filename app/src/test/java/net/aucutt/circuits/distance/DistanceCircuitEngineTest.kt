package net.aucutt.circuits.distance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.aucutt.circuits.ui.distance.DistanceConfig
import net.aucutt.circuits.ui.distance.DistanceMiles
import net.aucutt.circuits.ui.distance.DistancePhase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DistanceCircuitEngineTest {

    private lateinit var scheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        DistanceCircuitEngine.resetForTests()
        scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        DistanceCircuitEngine.setTickerScopeForTests(CoroutineScope(SupervisorJob() + dispatcher))
    }

    @After
    fun tearDown() {
        DistanceCircuitEngine.resetForTests()
        DistanceCircuitEngine.restoreDefaultTickerScopeForTests()
    }

    private fun advanceTime(millis: Long) {
        scheduler.advanceTimeBy(millis + 1)
    }

    @Test
    fun start_entersPreWorkoutAt30Seconds() {
        DistanceCircuitEngine.applyConfig(DistanceConfig(halfMiles = 4, cooldownMinutes = 1))
        DistanceCircuitEngine.start()

        val state = DistanceCircuitEngine.uiState.value
        assertEquals(DistancePhase.PreWorkout, state.phase)
        assertEquals(30, state.remainingSeconds)
        assertTrue(state.isRunning)
    }

    @Test
    fun afterPreWorkoutCountdown_emitsWorkoutStart() {
        val dispatcher = StandardTestDispatcher(scheduler)
        val announcements = mutableListOf<DistanceAnnouncement>()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val job = scope.launch {
            DistanceCircuitEngine.announcements.collect { announcements.add(it) }
        }
        scheduler.runCurrent()

        DistanceCircuitEngine.applyConfig(DistanceConfig(halfMiles = 4, cooldownMinutes = 1))
        DistanceCircuitEngine.start()
        advanceTime(30_000)
        scheduler.runCurrent()
        job.cancel()

        assertTrue(announcements.contains(DistanceAnnouncement.PreWorkout))
        assertTrue(announcements.contains(DistanceAnnouncement.WorkStart(1)))
    }

    @Test
    fun afterPreWorkoutCountdown_transitionsToWork() {
        DistanceCircuitEngine.applyConfig(DistanceConfig(halfMiles = 4, cooldownMinutes = 1))
        DistanceCircuitEngine.start()
        advanceTime(30_000)

        val state = DistanceCircuitEngine.uiState.value
        assertEquals(DistancePhase.Work, state.phase)
        assertEquals(1, state.currentRound)
        assertEquals(0.0, state.distanceMeters, 0.001)
        assertFalse(state.hasGpsFix)
    }

    @Test
    fun addDistance_announcesEachHalfMileMarker() {
        val dispatcher = StandardTestDispatcher(scheduler)
        val announcements = mutableListOf<DistanceAnnouncement>()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val job = scope.launch {
            DistanceCircuitEngine.announcements.collect { announcements.add(it) }
        }
        scheduler.runCurrent()

        DistanceCircuitEngine.applyConfig(DistanceConfig(halfMiles = 4, cooldownMinutes = 1))
        DistanceCircuitEngine.start()
        advanceTime(30_000)
        DistanceCircuitEngine.addDistance(DistanceMiles.toMeters(4))

        scheduler.runCurrent()
        job.cancel()

        assertEquals(
            listOf(
                DistanceAnnouncement.HalfMileMarker(1),
                DistanceAnnouncement.HalfMileMarker(2),
                DistanceAnnouncement.HalfMileMarker(3),
                DistanceAnnouncement.HalfMileMarker(4),
            ),
            announcements.filterIsInstance<DistanceAnnouncement.HalfMileMarker>(),
        )
    }

    @Test
    fun addDistance_reachingTarget_transitionsToCooldown() {
        DistanceCircuitEngine.applyConfig(DistanceConfig(halfMiles = 2, cooldownMinutes = 1))
        DistanceCircuitEngine.start()
        advanceTime(30_000)

        DistanceCircuitEngine.addDistance(DistanceMiles.toMeters(2))

        val state = DistanceCircuitEngine.uiState.value
        assertEquals(DistancePhase.Cooldown, state.phase)
        assertEquals(60, state.remainingSeconds)
    }

    @Test
    fun addDistance_withoutCooldown_finishesImmediately() {
        DistanceCircuitEngine.applyConfig(
            DistanceConfig(halfMiles = 2, cooldownMinutes = 0, repeats = 1),
        )
        DistanceCircuitEngine.start()
        advanceTime(30_000)

        DistanceCircuitEngine.addDistance(DistanceMiles.toMeters(2))

        assertEquals(DistancePhase.Finished, DistanceCircuitEngine.uiState.value.phase)
    }

    @Test
    fun cooldownCountdown_finishesCircuit() {
        DistanceCircuitEngine.applyConfig(
            DistanceConfig(halfMiles = 2, cooldownMinutes = 1, repeats = 1),
        )
        DistanceCircuitEngine.start()
        advanceTime(30_000)
        DistanceCircuitEngine.addDistance(DistanceMiles.toMeters(2))

        advanceTime(60_000)

        assertEquals(DistancePhase.Finished, DistanceCircuitEngine.uiState.value.phase)
    }

    @Test
    fun workWithoutCooldown_advancesToNextRound() {
        DistanceCircuitEngine.applyConfig(
            DistanceConfig(halfMiles = 2, cooldownMinutes = 0, repeats = 2),
        )
        DistanceCircuitEngine.start()
        advanceTime(30_000)
        DistanceCircuitEngine.addDistance(DistanceMiles.toMeters(2))

        val state = DistanceCircuitEngine.uiState.value
        assertEquals(DistancePhase.Work, state.phase)
        assertEquals(2, state.currentRound)
        assertEquals(0.0, state.distanceMeters, 0.001)
    }

    @Test
    fun pause_ignoresDistanceUpdates() {
        DistanceCircuitEngine.applyConfig(DistanceConfig(halfMiles = 4, cooldownMinutes = 1))
        DistanceCircuitEngine.start()
        advanceTime(30_000)
        DistanceCircuitEngine.pause()

        DistanceCircuitEngine.addDistance(500.0)

        assertEquals(0.0, DistanceCircuitEngine.uiState.value.distanceMeters, 0.001)
        assertTrue(DistanceCircuitEngine.uiState.value.isPaused)
    }
}
