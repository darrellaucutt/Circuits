package net.aucutt.circuits.ui.distance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import net.aucutt.circuits.ui.components.BannerBackground
import net.aucutt.circuits.ui.components.ValueStepper
import net.aucutt.circuits.ui.theme.CircuitCyan
import net.aucutt.circuits.ui.theme.CircuitsTheme
import net.aucutt.circuits.ui.theme.OnPrimaryLight
import net.aucutt.circuits.ui.theme.RunnerOrange

@Composable
fun DistanceCircuitScreen(
    onNavigateBack: () -> Unit,
    viewModel: DistanceCircuitViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.start()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificationsGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@rememberLauncherForActivityResult
            }
        }
        viewModel.start()
    }

    fun startCircuit() {
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!locationGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificationsGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.start()
    }

    val canNavigateBack = uiState.phase == DistancePhase.Idle
    BackHandler(enabled = canNavigateBack, onBack = onNavigateBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BannerBackground()

            CompositionLocalProvider(LocalContentColor provides OnPrimaryLight) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (canNavigateBack) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }

                    when (uiState.phase) {
                        DistancePhase.Idle -> DistanceSetup(
                            halfMiles = uiState.config.halfMiles,
                            cooldownMinutes = uiState.config.cooldownMinutes,
                            repeats = uiState.config.repeats,
                            onHalfMilesChange = viewModel::updateHalfMiles,
                            onCooldownChange = viewModel::updateCooldown,
                            onRepeatsChange = viewModel::updateRepeats,
                            onStart = ::startCircuit,
                            modifier = Modifier.weight(1f),
                        )

                        DistancePhase.Work, DistancePhase.Cooldown, DistancePhase.PreWorkout -> DistanceActiveWorkout(
                            uiState = uiState,
                            onPause = viewModel::pause,
                            onResume = viewModel::resume,
                            onStop = viewModel::stop,
                            modifier = Modifier.weight(1f),
                        )

                        DistancePhase.Finished -> DistanceCompleteWorkout(
                            repeats = uiState.config.repeats,
                            onAgain = viewModel::resetToSetup,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceSetup(
    halfMiles: Int,
    cooldownMinutes: Int,
    repeats: Int,
    onHalfMilesChange: (Int) -> Unit,
    onCooldownChange: (Int) -> Unit,
    onRepeatsChange: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(108.dp))

        Text(
            text = stringResource(R.string.mode_distance_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.distance_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        ValueStepper(
            label = stringResource(R.string.label_distance),
            valueText = DistanceMiles.format(halfMiles),
            unit = stringResource(R.string.unit_miles),
            onDecrement = { onHalfMilesChange(halfMiles - DistanceMiles.STEP_HALF_MILES) },
            onIncrement = { onHalfMilesChange(halfMiles + DistanceMiles.STEP_HALF_MILES) },
            canDecrement = halfMiles > DistanceMiles.MIN_HALF_MILES,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        ValueStepper(
            label = stringResource(R.string.label_cooldown),
            valueText = cooldownMinutes.toString(),
            unit = stringResource(R.string.unit_minutes),
            onDecrement = { onCooldownChange(cooldownMinutes - 1) },
            onIncrement = { onCooldownChange(cooldownMinutes + 1) },
            canDecrement = cooldownMinutes > 0,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        ValueStepper(
            label = stringResource(R.string.label_repeats),
            valueText = repeats.toString(),
            unit = stringResource(R.string.unit_times),
            onDecrement = { onRepeatsChange(repeats - 1) },
            onIncrement = { onRepeatsChange(repeats + 1) },
            canDecrement = repeats > 1,
            modifier = Modifier.widthIn(max = 360.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CircuitCyan,
                contentColor = OnPrimaryLight,
            ),
        ) {
            Text(stringResource(R.string.action_start))
        }
    }
}

@Composable
private fun DistanceActiveWorkout(
    uiState: DistanceUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val phaseLabel = when (uiState.phase) {
        DistancePhase.PreWorkout -> stringResource(R.string.phase_pre_workout)
        DistancePhase.Work -> stringResource(R.string.phase_work)
        DistancePhase.Cooldown -> stringResource(R.string.phase_cooldown)
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
            fontWeight = FontWeight.Bold,
        )

        if (uiState.phase != DistancePhase.PreWorkout) {
            Text(
                text = stringResource(
                    R.string.round_progress,
                    uiState.currentRound,
                    uiState.config.repeats,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        when (uiState.phase) {
            DistancePhase.PreWorkout -> {
                Text(
                    text = stringResource(R.string.pre_workout_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatTime(uiState.remainingSeconds),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    fontWeight = FontWeight.Bold,
                )
            }

            DistancePhase.Work -> {
                if (uiState.hasGpsFix) {
                    Text(
                        text = stringResource(R.string.distance_remaining_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.distance_remaining_value,
                            DistanceMiles.formatDecimal(
                                uiState.remainingWorkMeters / DistanceMiles.METERS_PER_MILE,
                            ),
                        ),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            R.string.distance_progress,
                            DistanceMiles.formatDecimal(
                                uiState.distanceMeters / DistanceMiles.METERS_PER_MILE,
                            ),
                            DistanceMiles.format(uiState.config.halfMiles),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.status_acquiring_gps),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            DistancePhase.Cooldown -> {
                Text(
                    text = formatTime(uiState.remainingSeconds),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    fontWeight = FontWeight.Bold,
                )
            }

            else -> Unit
        }

        if (uiState.isPaused) {
            Text(
                text = stringResource(R.string.status_paused),
                style = MaterialTheme.typography.titleMedium,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CircuitCyan,
                        contentColor = OnPrimaryLight,
                    ),
                ) {
                    Text(stringResource(R.string.action_resume))
                }
            } else {
                FilledTonalButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(contentColor = OnPrimaryLight),
                ) {
                    Text(stringResource(R.string.action_pause))
                }
            }
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnPrimaryLight),
                border = BorderStroke(1.5.dp, OnPrimaryLight),
            ) {
                Text(stringResource(R.string.action_stop))
            }
        }
    }
}

@Composable
private fun DistanceCompleteWorkout(
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
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RunnerOrange,
                contentColor = OnPrimaryLight,
            ),
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
private fun DistanceSetupPreview() {
    CircuitsTheme {
        DistanceSetup(
            halfMiles = 4,
            cooldownMinutes = 1,
            repeats = 8,
            onHalfMilesChange = {},
            onCooldownChange = {},
            onRepeatsChange = {},
            onStart = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DistanceActivePreview() {
    CircuitsTheme {
        DistanceActiveWorkout(
            uiState = DistanceUiState(
                config = DistanceConfig(halfMiles = 4, cooldownMinutes = 1, repeats = 8),
                phase = DistancePhase.Work,
                distanceMeters = DistanceMiles.toMeters(2),
                currentRound = 3,
                hasGpsFix = true,
            ),
            onPause = {},
            onResume = {},
            onStop = {},
        )
    }
}
