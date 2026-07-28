package net.aucutt.circuits.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.aucutt.circuits.R
import net.aucutt.circuits.data.CircuitEntity
import net.aucutt.circuits.ui.theme.CircuitsTheme

@Composable
fun CircuitTimerScreen(
    viewModel: CircuitTimerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedCircuits by viewModel.savedCircuits.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    val loadedName by viewModel.loadedName.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showLoadDialog by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.start()
    }

    fun startCircuit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.start()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (uiState.phase) {
            TimerPhase.Idle -> SetupWorkout(
                config = uiState.config,
                canSave = isDirty || loadedName.isEmpty(),
                canLoad = savedCircuits.isNotEmpty(),
                onIntervalChange = viewModel::updateInterval,
                onCooldownChange = viewModel::updateCooldown,
                onRepeatsChange = viewModel::updateRepeats,
                onStart = ::startCircuit,
                onLoad = { showLoadDialog = true },
                onSave = { showSaveDialog = true },
                modifier = Modifier.padding(innerPadding),
            )

            TimerPhase.Work, TimerPhase.Cooldown -> StartWorkout(
                uiState = uiState,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
                modifier = Modifier.padding(innerPadding),
            )

            TimerPhase.Finished -> CompleteWorkout(
                repeats = uiState.config.repeats,
                onAgain = viewModel::resetToSetup,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showSaveDialog) {
        SaveCircuitDialog(
            initialName = loadedName,
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                viewModel.saveCircuit(name)
                showSaveDialog = false
            },
        )
    }

    if (showLoadDialog) {
        LoadCircuitDialog(
            circuits = savedCircuits,
            onDismiss = { showLoadDialog = false },
            onSelect = { circuit ->
                viewModel.loadCircuit(circuit)
                showLoadDialog = false
            },
        )
    }
}

@Composable
private fun SetupWorkout(
    config: TimerConfig,
    canSave: Boolean,
    canLoad: Boolean,
    onIntervalChange: (Int) -> Unit,
    onCooldownChange: (Int) -> Unit,
    onRepeatsChange: (Int) -> Unit,
    onStart: () -> Unit,
    onLoad: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        NumberStepper(
            label = stringResource(R.string.label_interval),
            value = config.intervalMinutes,
            unit = stringResource(R.string.unit_minutes),
            onDecrement = { onIntervalChange(config.intervalMinutes - 1) },
            onIncrement = { onIntervalChange(config.intervalMinutes + 1) },
            canDecrement = config.intervalMinutes > 1,
        )
        NumberStepper(
            label = stringResource(R.string.label_cooldown),
            value = config.cooldownMinutes,
            unit = stringResource(R.string.unit_minutes),
            onDecrement = { onCooldownChange(config.cooldownMinutes - 1) },
            onIncrement = { onCooldownChange(config.cooldownMinutes + 1) },
            canDecrement = config.cooldownMinutes > 0,
        )
        NumberStepper(
            label = stringResource(R.string.label_repeats),
            value = config.repeats,
            unit = stringResource(R.string.unit_times),
            onDecrement = { onRepeatsChange(config.repeats - 1) },
            onIncrement = { onRepeatsChange(config.repeats + 1) },
            canDecrement = config.repeats > 1,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
        ) {
            Text(stringResource(R.string.action_start))
        }

        OutlinedButton(
            onClick = onLoad,
            enabled = canLoad,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
        ) {
            Text(stringResource(R.string.action_load))
        }

        FilledTonalButton(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@Composable
private fun SaveCircuitDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.save_dialog_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun LoadCircuitDialog(
    circuits: List<CircuitEntity>,
    onDismiss: () -> Unit,
    onSelect: (CircuitEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.load_dialog_title)) },
        text = {
            if (circuits.isEmpty()) {
                Text(stringResource(R.string.load_dialog_empty))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(circuits, key = { it.id }) { circuit ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(circuit) }
                                .padding(vertical = 12.dp),
                        ) {
                            Text(
                                text = circuit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(
                                    R.string.circuit_summary,
                                    circuit.intervalMinutes,
                                    circuit.cooldownMinutes,
                                    circuit.repeats,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    unit: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onDecrement,
                enabled = canDecrement,
            ) {
                Text("−", fontSize = 22.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onIncrement) {
                Text("+", fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun StartWorkout(
    uiState: TimerUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val phaseLabel = when (uiState.phase) {
        TimerPhase.Work -> stringResource(R.string.phase_work)
        TimerPhase.Cooldown -> stringResource(R.string.phase_cooldown)
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = if (uiState.phase == TimerPhase.Work) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(
                R.string.round_progress,
                uiState.currentRound,
                uiState.config.repeats,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatTime(uiState.remainingSeconds),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
            fontWeight = FontWeight.Bold,
        )
        if (uiState.isPaused) {
            Text(
                text = stringResource(R.string.status_paused),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isPaused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_resume))
                }
            } else {
                FilledTonalButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_pause))
                }
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_stop))
            }
        }
    }
}

@Composable
private fun CompleteWorkout(
    repeats: Int,
    onAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.finished_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.finished_subtitle, repeats),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
        ) {
            Text(stringResource(R.string.action_again))
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun SetupPreview() {
    CircuitsTheme {
        SetupWorkout(
            config = TimerConfig(),
            canSave = true,
            canLoad = true,
            onIntervalChange = {},
            onCooldownChange = {},
            onRepeatsChange = {},
            onStart = {},
            onLoad = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivePreview() {
    CircuitsTheme {
        StartWorkout(
            uiState = TimerUiState(
                config = TimerConfig(),
                phase = TimerPhase.Work,
                remainingSeconds = 37,
                currentRound = 3,
            ),
            onPause = {},
            onResume = {},
            onStop = {},
        )
    }
}
