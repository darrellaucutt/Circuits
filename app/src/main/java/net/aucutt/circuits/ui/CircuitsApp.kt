package net.aucutt.circuits.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import net.aucutt.circuits.ui.distance.DistanceCircuitScreen
import net.aucutt.circuits.ui.mode.ModeSelectionScreen
import net.aucutt.circuits.ui.timer.CircuitTimerScreen

private enum class AppDestination {
    ModeSelection,
    TimedCircuits,
    DistanceCircuits,
}

@Composable
fun CircuitsApp() {
    var destination by rememberSaveable { mutableStateOf(AppDestination.ModeSelection) }

    when (destination) {
        AppDestination.ModeSelection -> ModeSelectionScreen(
            onTimedSelected = { destination = AppDestination.TimedCircuits },
            onDistanceSelected = { destination = AppDestination.DistanceCircuits },
        )

        AppDestination.TimedCircuits -> CircuitTimerScreen(
            onNavigateBack = { destination = AppDestination.ModeSelection },
        )

        AppDestination.DistanceCircuits -> DistanceCircuitScreen(
            onNavigateBack = { destination = AppDestination.ModeSelection },
        )
    }
}
