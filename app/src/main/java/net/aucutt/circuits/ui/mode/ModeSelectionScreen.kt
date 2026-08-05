package net.aucutt.circuits.ui.mode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aucutt.circuits.R
import net.aucutt.circuits.ui.components.BannerBackground
import net.aucutt.circuits.ui.theme.BannerSurfaceVariant
import net.aucutt.circuits.ui.theme.CircuitCyan
import net.aucutt.circuits.ui.theme.CircuitsTheme
import net.aucutt.circuits.ui.theme.OnPrimaryLight
import net.aucutt.circuits.ui.theme.RunnerOrange

@Composable
fun ModeSelectionScreen(
    onTimedSelected: () -> Unit,
    onDistanceSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(160.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.mode_selection_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ModeOptionCard(
                            title = stringResource(R.string.mode_timed_title),
                            description = stringResource(R.string.mode_timed_description),
                            icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                            accentColor = CircuitCyan,
                            onClick = onTimedSelected,
                        )
                        ModeOptionCard(
                            title = stringResource(R.string.mode_distance_title),
                            description = stringResource(R.string.mode_distance_description),
                            icon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                            accentColor = RunnerOrange,
                            onClick = onDistanceSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BannerSurfaceVariant.copy(alpha = 0.92f),
            contentColor = OnPrimaryLight,
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides accentColor) {
                    icon()
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = OnPrimaryLight.copy(alpha = 0.85f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeSelectionPreview() {
    CircuitsTheme {
        ModeSelectionScreen(
            onTimedSelected = {},
            onDistanceSelected = {},
        )
    }
}
