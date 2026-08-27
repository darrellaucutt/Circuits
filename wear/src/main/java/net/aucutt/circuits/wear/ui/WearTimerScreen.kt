package net.aucutt.circuits.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import net.aucutt.circuits.model.TimerPhase
import net.aucutt.circuits.model.TimerUiState
import net.aucutt.circuits.sync.SyncCircuit
import net.aucutt.circuits.sync.SyncConfig
import net.aucutt.circuits.wear.R

@Composable
fun WearTimerScreen(
    viewModel: WearTimerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val circuits by viewModel.circuits.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeConfig.collectAsStateWithLifecycle()
    val selectedCircuitId by viewModel.selectedCircuitId.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        TimeText(modifier = Modifier.align(Alignment.TopCenter))

        when (uiState.phase) {
            TimerPhase.Idle -> WearIdleScreen(
                circuits = circuits,
                activeConfig = activeConfig,
                selectedCircuitId = selectedCircuitId,
                onSelectCircuit = viewModel::selectCircuit,
                onStart = viewModel::start,
                configSummary = viewModel::configSummary,
            )

            TimerPhase.PreWorkout, TimerPhase.Work, TimerPhase.Cooldown -> WearRunningScreen(
                uiState = uiState,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
            )

            TimerPhase.Finished -> WearFinishedScreen(
                repeats = uiState.config.repeats,
                onAgain = viewModel::resetToSetup,
            )
        }
    }
}

@Composable
private fun WearIdleScreen(
    circuits: List<SyncCircuit>,
    activeConfig: SyncConfig?,
    selectedCircuitId: Long?,
    onSelectCircuit: (Long) -> Unit,
    onStart: () -> Unit,
    configSummary: (SyncConfig) -> String,
) {
    val selectedConfig = remember(circuits, activeConfig, selectedCircuitId) {
        selectedCircuitId?.let { id -> circuits.firstOrNull { it.id == id } }
            ?.let { circuit ->
                SyncConfig(
                    name = circuit.name,
                    intervalMinutes = circuit.intervalMinutes,
                    cooldownMinutes = circuit.cooldownMinutes,
                    repeats = circuit.repeats,
                )
            } ?: activeConfig
    }

    if (circuits.size > 1) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(circuits, key = { it.id }) { circuit ->
                val selected = circuit.id == selectedCircuitId
                Button(
                    onClick = { onSelectCircuit(circuit.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = circuit.name)
                        Text(
                            text = configSummary(
                                SyncConfig(
                                    name = circuit.name,
                                    intervalMinutes = circuit.intervalMinutes,
                                    cooldownMinutes = circuit.cooldownMinutes,
                                    repeats = circuit.repeats,
                                ),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (selected) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onStart,
                    enabled = selectedConfig != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_start))
                }
            }
            if (selectedConfig == null) {
                item {
                    Text(
                        text = stringResource(R.string.wear_no_circuits),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    } else {
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
            if (selectedConfig != null) {
                Text(
                    text = selectedConfig.name,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = configSummary(selectedConfig),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Button(onClick = onStart) {
                    Text(stringResource(R.string.action_start))
                }
            } else {
                Text(
                    text = stringResource(R.string.wear_no_circuits),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Text(
                    text = stringResource(R.string.wear_idle_hint),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WearRunningScreen(
    uiState: TimerUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val phaseLabel = when (uiState.phase) {
        TimerPhase.PreWorkout -> stringResource(R.string.phase_pre_workout)
        TimerPhase.Work -> stringResource(R.string.phase_work)
        TimerPhase.Cooldown -> stringResource(R.string.phase_cooldown)
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (uiState.phase != TimerPhase.PreWorkout) {
            Text(
                text = stringResource(
                    R.string.round_progress,
                    uiState.currentRound,
                    uiState.config.repeats,
                ),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = formatTime(uiState.remainingSeconds),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )
        if (uiState.isPaused) {
            Text(
                text = stringResource(R.string.status_paused),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        if (uiState.isPaused) {
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
private fun WearFinishedScreen(
    repeats: Int,
    onAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.finished_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.finished_subtitle, repeats),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Button(onClick = onAgain) {
            Text(stringResource(R.string.action_again))
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
