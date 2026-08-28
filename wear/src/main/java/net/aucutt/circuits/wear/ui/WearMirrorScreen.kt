package net.aucutt.circuits.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import net.aucutt.circuits.sync.SyncTimerState
import net.aucutt.circuits.wear.R

@Composable
fun WearMirrorScreen(
    viewModel: WearMirrorViewModel = viewModel(),
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val phoneConnected by viewModel.phoneConnected.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        TimeText(modifier = Modifier.align(Alignment.TopCenter))

        when {
            timerState == null || !phoneConnected -> WaitingScreen(
                hasState = timerState != null,
            )
            timerState!!.phase == "Idle" -> WaitingScreen(hasState = true)
            timerState!!.phase == "Finished" -> FinishedScreen(state = timerState!!)
            else -> RunningScreen(
                state = timerState!!,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
                onStateDisplayed = viewModel::onStateDisplayed,
            )
        }
    }
}

@Composable
private fun WaitingScreen(hasState: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (hasState) {
                stringResource(R.string.wear_waiting)
            } else {
                stringResource(R.string.wear_disconnected)
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun RunningScreen(
    state: SyncTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onStateDisplayed: (phase: String, round: Int) -> Unit,
) {
    LaunchedEffect(state.phase, state.currentRound, state.updatedAt) {
        onStateDisplayed(state.phase, state.currentRound)
    }

    val phaseLabel = when (state.phase) {
        "PreWorkout" -> stringResource(R.string.phase_pre_workout)
        "Work" -> stringResource(R.string.phase_work)
        "Cooldown" -> stringResource(R.string.phase_cooldown)
        else -> state.phase
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.circuitName.isNotBlank()) {
            Text(
                text = state.circuitName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (state.phase != "PreWorkout") {
            Text(
                text = stringResource(
                    R.string.round_progress,
                    state.currentRound,
                    state.repeats,
                ),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = formatTime(state.remainingSeconds),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (state.isPaused) {
            Text(
                text = stringResource(R.string.status_paused),
                style = MaterialTheme.typography.labelSmall,
            )
            Button(onClick = onResume, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.action_resume))
            }
        } else {
            Button(onClick = onPause, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.action_pause))
            }
        }
        Button(onClick = onStop, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.action_stop))
        }
    }
}

@Composable
private fun FinishedScreen(state: SyncTimerState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.phase_finished),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.round_progress, state.repeats, state.repeats),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
